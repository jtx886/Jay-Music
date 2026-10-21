<?php
/**
 * POST /api/send_code.php
 * 请求体：{ "email": "xxx@xx.com", "scene": "register|reset" }
 * 返回：{ code, message, data: { cooldown, expiresIn } }
 */

declare(strict_types=1);

require_once __DIR__ . '/../helpers.php';
require_once __DIR__ . '/../mailer.php';

require_method('POST');

try {
    $data = body();
    $email = assert_email(str_field($data, 'email'));
    $scene = str_field($data, 'scene', 'register');

    if (!in_array($scene, ['register', 'reset'], true)) {
        fail('场景参数不正确');
    }

    $pdo = db();
    $now = time();

    // 注册场景要求邮箱未注册；重置场景要求邮箱已注册
    $stmt = $pdo->prepare('SELECT id FROM users WHERE email = ? LIMIT 1');
    $stmt->execute([$email]);
    $exists = (bool)$stmt->fetch();

    if ($scene === 'register' && $exists) {
        fail('该邮箱已注册，请直接登录');
    }
    if ($scene === 'reset' && !$exists) {
        fail('该邮箱尚未注册');
    }

    // 同一邮箱同一场景的发送间隔限制
    $stmt = $pdo->prepare(
        'SELECT created_at FROM email_codes WHERE email = ? AND scene = ? ORDER BY id DESC LIMIT 1'
    );
    $stmt->execute([$email, $scene]);
    $lastCreatedAt = $stmt->fetchColumn();

    if ($lastCreatedAt !== false) {
        $elapsed = $now - (int)$lastCreatedAt;
        if ($elapsed < CODE_COOLDOWN) {
            $wait = CODE_COOLDOWN - $elapsed;
            respond(429, "发送太频繁，请 {$wait} 秒后再试", [
                'cooldown' => $wait,
                'expiresIn' => CODE_TTL,
            ]);
        }
    }

    // 单邮箱每日发送上限，防止被恶意刷邮件
    $stmt = $pdo->prepare('SELECT COUNT(*) FROM email_codes WHERE email = ? AND created_at > ?');
    $stmt->execute([$email, $now - 86400]);
    if ((int)$stmt->fetchColumn() >= 10) {
        fail('今日验证码发送次数已达上限，请明天再试', 429);
    }

    $code = str_pad((string)random_int(0, 999999), 6, '0', STR_PAD_LEFT);

    $pdo->prepare(
        'INSERT INTO email_codes (email, code, scene, attempts, used, expires_at, created_at)
         VALUES (?, ?, ?, 0, 0, ?, ?)'
    )->execute([$email, $code, $scene, $now + CODE_TTL, $now]);
    $newCodeId = (int)$pdo->lastInsertId();

    $html = verification_mail_html($code, $scene, $email);

    try {
        (new SmtpMailer())->send($email, '【Jay Music】登录验证码', $html);
    } catch (Throwable $e) {
        // 发送失败时只回滚本次验证码，旧的验证码仍然可用
        $pdo->prepare('DELETE FROM email_codes WHERE id = ?')->execute([$newCodeId]);
        error_log('[JayMusic] send mail failed: ' . $e->getMessage());
        fail('验证码邮件发送失败，请稍后重试', 500);
    }

    // 新验证码已成功送达，作废同场景下的旧验证码
    $pdo->prepare(
        'UPDATE email_codes SET used = 1 WHERE email = ? AND scene = ? AND id <> ?'
    )->execute([$email, $scene, $newCodeId]);

    prune_codes($pdo);

    ok('验证码已发送，请注意查收', [
        'cooldown'  => CODE_COOLDOWN,
        'expiresIn' => CODE_TTL,
    ]);
} catch (Throwable $e) {
    error_log('[JayMusic] send_code error: ' . $e->getMessage());
    fail('服务开小差了，请稍后重试', 500);
}
