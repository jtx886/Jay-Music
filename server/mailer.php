<?php
/**
 * 轻量 SMTP 邮件发送（无需 Composer 依赖）
 * 支持 465 端口 SMTPS（SSL）与 587 端口 STARTTLS。
 * 仅后端使用，Android 客户端不执行任何邮件发送逻辑。
 */

declare(strict_types=1);

require_once __DIR__ . '/config.php';

final class SmtpMailer
{
    /** @var resource|null */
    private $socket = null;

    private int $timeout = 20;

    public function __construct(
        private readonly string $host = SMTP_HOST,
        private readonly int $port = SMTP_PORT,
        private readonly string $user = SMTP_USER,
        private readonly string $pass = SMTP_PASS,
        private readonly string $from = SMTP_FROM,
        private readonly string $fromName = SMTP_FROM_NAME
    ) {
    }

    /**
     * 发送 HTML 邮件
     *
     * @throws RuntimeException
     */
    public function send(string $to, string $subject, string $html): void
    {
        if ($this->pass === '' || $this->user === '') {
            throw new RuntimeException('SMTP 账号未配置');
        }

        try {
            $this->connect();
            $this->command('MAIL FROM:<' . $this->from . '>', [250]);
            $this->command('RCPT TO:<' . $to . '>', [250, 251]);
            $this->command('DATA', [354]);
            $message = $this->buildMessage($to, $subject, $html);
            $this->write($message . "\r\n.\r\n");
            $this->expect([250]);
            $this->quit();
        } finally {
            if (is_resource($this->socket)) {
                @fclose($this->socket);
            }
            $this->socket = null;
        }
    }

    // ------------------------- 连接与握手 -------------------------

    private function connect(): void
    {
        $transport = $this->port === 465 ? 'ssl://' : 'tcp://';
        $context = stream_context_create([
            'ssl' => [
                'verify_peer'      => true,
                'verify_peer_name' => true,
                'allow_self_signed'=> false,
            ],
        ]);

        $errno = 0;
        $errstr = '';
        $socket = @stream_socket_client(
            $transport . $this->host . ':' . $this->port,
            $errno,
            $errstr,
            $this->timeout,
            STREAM_CLIENT_CONNECT,
            $context
        );

        if (!is_resource($socket)) {
            throw new RuntimeException("连接邮件服务器失败（{$errno}: {$errstr}）");
        }

        $this->socket = $socket;
        stream_set_timeout($this->socket, $this->timeout);

        $this->expect([220]);
        $this->ehlo();

        if ($this->port !== 465) {
            $this->command('STARTTLS', [220]);
            $ok = stream_socket_enable_crypto(
                $this->socket,
                true,
                STREAM_CRYPTO_METHOD_TLS_CLIENT
            );
            if ($ok !== true) {
                throw new RuntimeException('启用 TLS 加密失败');
            }
            $this->ehlo();
        }

        $this->command('AUTH LOGIN', [334]);
        $this->command(base64_encode($this->user), [334]);
        $this->command(base64_encode($this->pass), [235]);
    }

    private function ehlo(): void
    {
        $hostname = gethostname() ?: 'localhost';
        $this->command('EHLO ' . $hostname, [250]);
    }

    private function quit(): void
    {
        try {
            $this->command('QUIT', [221]);
        } catch (RuntimeException) {
            // 退出失败不影响邮件投递结果
        }
    }

    // ------------------------- 协议读写 -------------------------

    private function write(string $data): void
    {
        if (!is_resource($this->socket)) {
            throw new RuntimeException('SMTP 连接已关闭');
        }
        if (@fwrite($this->socket, $data) === false) {
            throw new RuntimeException('写入邮件服务器失败');
        }
    }

    /**
     * 发送命令并校验预期响应码
     */
    private function command(string $line, array $expected): void
    {
        $this->write($line . "\r\n");
        $this->expect($expected);
    }

    private function expect(array $expected): void
    {
        [$code, $text] = $this->readResponse();
        if (!in_array($code, $expected, true)) {
            throw new RuntimeException("邮件服务器返回异常（{$code}）：{$text}");
        }
    }

    /**
     * @return array{0:int,1:string}
     */
    private function readResponse(): array
    {
        if (!is_resource($this->socket)) {
            throw new RuntimeException('SMTP 连接已关闭');
        }

        $lines = [];
        while (($line = fgets($this->socket, 1024)) !== false) {
            $line = rtrim($line, "\r\n");
            $lines[] = $line;
            // 形如 "250-XXX" 表示还有后续行，形如 "250 XXX" 表示结束
            if (strlen($line) < 4 || $line[3] === ' ') {
                break;
            }
        }

        if ($lines === []) {
            throw new RuntimeException('邮件服务器无响应');
        }

        return [(int)substr($lines[0], 0, 3), implode(' | ', $lines)];
    }

    // ------------------------- 邮件内容 -------------------------

    private function buildMessage(string $to, string $subject, string $html): string
    {
        $host = gethostname() ?: 'localhost';
        $encodedFrom = $this->encodeHeader($this->fromName);
        $encodedSubject = $this->encodeHeader($subject);

        $headers = [
            'Date: ' . date('r'),
            'From: ' . $encodedFrom . ' <' . $this->from . '>',
            'To: <' . $to . '>',
            'Subject: ' . $encodedSubject,
            'Message-ID: <' . bin2hex(random_bytes(12)) . '@' . $host . '>',
            'MIME-Version: 1.0',
            'Content-Type: text/html; charset=UTF-8',
            'Content-Transfer-Encoding: base64',
            'X-Mailer: JayMusic-Mailer',
        ];

        $body = chunk_split(base64_encode($html), 76, "\r\n");

        return implode("\r\n", $headers) . "\r\n\r\n" . $body;
    }

    private function encodeHeader(string $value): string
    {
        // 纯 ASCII 内容直接输出，含中文时使用 RFC 2047 编码
        if (preg_match('/^[\x20-\x7E]*$/', $value)) {
            return $value;
        }
        return '=?UTF-8?B?' . base64_encode($value) . '?=';
    }
}

/**
 * 验证码邮件模板
 *
 * @param string $scene register / reset
 */
function verification_mail_html(string $code, string $scene, string $email): string
{
    $title = $scene === 'reset' ? '重置登录密码' : '注册 Jay Music 账号';
    $action = $scene === 'reset' ? '重置密码' : '完成注册';
    $minutes = (int)ceil(CODE_TTL / 60);
    $safeCode = htmlspecialchars($code, ENT_QUOTES, 'UTF-8');
    $safeEmail = htmlspecialchars($email, ENT_QUOTES, 'UTF-8');

    return <<<HTML
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Jay Music 验证码</title>
</head>
<body style="margin:0;padding:0;background:#0b0d12;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI','PingFang SC','Microsoft YaHei',sans-serif;">
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#0b0d12;padding:32px 16px;">
    <tr>
      <td align="center">
        <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:560px;background:#141824;border-radius:24px;overflow:hidden;border:1px solid #232838;">
          <tr>
            <td style="padding:36px 36px 20px;background:linear-gradient(135deg,#1DB954 0%,#0F9D58 45%,#7B61FF 100%);">
              <div style="font-size:26px;font-weight:700;color:#ffffff;letter-spacing:0.5px;">Jay Music</div>
              <div style="margin-top:6px;font-size:13px;color:rgba(255,255,255,0.85);">聆听，让每个瞬间都有旋律</div>
            </td>
          </tr>
          <tr>
            <td style="padding:36px;">
              <div style="font-size:20px;font-weight:600;color:#F5F7FA;">{$title}</div>
              <div style="margin-top:12px;font-size:14px;line-height:1.8;color:#A7B0C0;">
                你好，你正在使用 <strong style="color:#F5F7FA;">{$safeEmail}</strong> {$action}。
                请在下方输入验证码，验证码 <strong style="color:#F5F7FA;">{$minutes} 分钟内</strong>有效。
              </div>
              <div style="margin:28px 0;padding:22px;border-radius:18px;background:#0F1320;border:1px solid #2A3147;text-align:center;">
                <div style="font-size:34px;font-weight:700;letter-spacing:10px;color:#1DB954;font-family:'SF Mono',Consolas,monospace;">{$safeCode}</div>
              </div>
              <div style="font-size:13px;line-height:1.8;color:#7C8698;">
                若这不是你本人操作，请忽略本邮件，你的账号不会受到影响。
              </div>
            </td>
          </tr>
          <tr>
            <td style="padding:20px 36px 32px;border-top:1px solid #232838;font-size:12px;color:#5E6779;">
              此邮件由 Jay Music 系统自动发送，请勿直接回复。
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
HTML;
}
