<?php
/**
 * 公共工具：统一响应、请求体解析、参数校验、鉴权、验证码消费、令牌签发。
 * 约定：所有接口始终返回 HTTP 200，用业务 code 表示结果（0 成功，非 0 失败），
 *      便于 Android 端 Retrofit 统一解析。
 */

declare(strict_types=1);

require_once __DIR__ . '/config.php';
require_once __DIR__ . '/db.php';

date_default_timezone_set(APP_TIMEZONE);

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Headers: Content-Type, Authorization');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('X-Content-Type-Options: nosniff');

if (($_SERVER['REQUEST_METHOD'] ?? '') === 'OPTIONS') {
    http_response_code(204);
    exit;
}

// ==================== 统一响应 ====================

/**
 * 输出 JSON 并结束请求
 */
function respond(int $code, string $message, mixed $data = null, int $status = 200): void
{
    http_response_code($status);
    echo json_encode([
        'code'    => $code,
        'message' => $message,
        'data'    => $data,
    ], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}

function ok(string $message = '操作成功', mixed $data = null): void
{
    respond(0, $message, $data);
}

function fail(string $message, int $code = 1): void
{
    respond($code, $message);
}

// ==================== 请求解析 ====================

/**
 * 读取 JSON 请求体（兼容表单提交）
 */
function body(): array
{
    static $cache = null;
    if (is_array($cache)) {
        return $cache;
    }
    $raw = file_get_contents('php://input');
    $decoded = json_decode($raw ?: '', true);
    $cache = is_array($decoded) ? $decoded : $_POST;
    return $cache;
}

function str_field(array $data, string $key, string $default = ''): string
{
    $value = $data[$key] ?? $default;
    return is_string($value) ? trim($value) : $default;
}

function int_field(array $data, string $key, int $default = 0): int
{
    $value = $data[$key] ?? $default;
    return is_numeric($value) ? (int)$value : $default;
}

/**
 * 限制字符串长度，避免超长内容写入数据库
 */
function clip(string $value, int $max): string
{
    return mb_substr($value, 0, $max, 'UTF-8');
}

function require_method(string $method): void
{
    if (strtoupper($_SERVER['REQUEST_METHOD'] ?? '') !== strtoupper($method)) {
        respond(405, '请求方式不被允许', null, 405);
    }
}

// ==================== 参数校验 ====================

function valid_email(string $email): bool
{
    return (bool)filter_var($email, FILTER_VALIDATE_EMAIL);
}

function assert_email(string $email): string
{
    $email = strtolower(trim($email));
    if ($email === '' || !valid_email($email) || strlen($email) > 190) {
        fail('请输入正确的邮箱地址');
    }
    return $email;
}

function assert_username(string $username): string
{
    $username = trim($username);
    $length = mb_strlen($username, 'UTF-8');
    if ($length < 2 || $length > 20) {
        fail('用户名长度需为 2-20 个字符');
    }
    if (!preg_match('/^[\p{L}\p{N}_.\-]+$/u', $username)) {
        fail('用户名只能包含中英文、数字、下划线、点或短横线');
    }
    return $username;
}

function assert_password(string $password): string
{
    $length = strlen($password);
    if ($length < 6) {
        fail('密码长度至少 6 位');
    }
    if ($length > 64) {
        fail('密码长度不能超过 64 位');
    }
    return $password;
}

function assert_code(string $code): string
{
    if (!preg_match('/^\d{6}$/', $code)) {
        fail('验证码为 6 位数字');
    }
    return $code;
}

// ==================== 鉴权 ====================

/**
 * 读取 Authorization: Bearer {token}
 */
function bearer_token(): ?string
{
    $header = $_SERVER['HTTP_AUTHORIZATION']
        ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION']
        ?? '';

    if ($header === '' && function_exists('getallheaders')) {
        foreach (getallheaders() as $key => $value) {
            if (strcasecmp($key, 'Authorization') === 0) {
                $header = $value;
                break;
            }
        }
    }

    if (preg_match('/Bearer\s+(\S+)/i', $header, $matches)) {
        return $matches[1];
    }
    return null;
}

/**
 * 必须登录：校验令牌并返回对应用户
 */
function require_auth(): array
{
    $token = bearer_token();
    if ($token === null || $token === '') {
        respond(401, '登录状态已失效，请重新登录');
    }

    $stmt = db()->prepare(
        'SELECT u.* FROM auth_tokens t
         INNER JOIN users u ON u.id = t.user_id
         WHERE t.token = ? AND t.expires_at > ?
         LIMIT 1'
    );
    $stmt->execute([$token, time()]);
    $user = $stmt->fetch();

    if (!$user) {
        respond(401, '登录状态已失效，请重新登录');
    }
    return $user;
}

/**
 * 生成并保存登录令牌，同时清理该用户已过期的令牌
 */
function issue_token(PDO $pdo, int $userId): string
{
    $now = time();
    $token = bin2hex(random_bytes(32));

    $pdo->prepare('DELETE FROM auth_tokens WHERE expires_at <= ?')
        ->execute([$now]);

    $pdo->prepare(
        'INSERT INTO auth_tokens (user_id, token, expires_at, created_at) VALUES (?, ?, ?, ?)'
    )->execute([$userId, $token, $now + TOKEN_TTL, $now]);

    return $token;
}

/**
 * 用户信息的对外结构（绝不返回密码）
 */
function public_user(array $user): array
{
    return [
        'id'       => (int)$user['id'],
        'username' => (string)$user['username'],
        'email'    => (string)$user['email'],
        'avatar'   => $user['avatar'] !== null && $user['avatar'] !== '' ? (string)$user['avatar'] : null,
    ];
}

/**
 * 曲目信息的对外结构（收藏 / 历史共用）
 */
function song_payload(array $row): array
{
    return [
        'songId'    => (string)$row['song_id'],
        'songName'  => (string)$row['song_name'],
        'artist'    => (string)($row['artist'] ?? ''),
        'album'     => (string)($row['album'] ?? ''),
        'coverUrl'  => (string)($row['cover_url'] ?? ''),
        'source'    => (string)($row['source'] ?? ''),
        'duration'  => (int)($row['duration'] ?? 0),
        'createdAt' => (int)($row['created_at'] ?? 0),
        'playedAt'  => (int)($row['played_at'] ?? 0),
    ];
}

// ==================== 验证码 ====================

/**
 * 消费验证码：校验有效期、错误次数与内容，成功后标记为已使用。
 * 校验失败会直接终止请求。
 */
function consume_code(PDO $pdo, string $email, string $scene, string $code): void
{
    $stmt = $pdo->prepare(
        'SELECT id, code, attempts, expires_at
         FROM email_codes
         WHERE email = ? AND scene = ? AND used = 0
         ORDER BY id DESC LIMIT 1'
    );
    $stmt->execute([$email, $scene]);
    $row = $stmt->fetch();

    if (!$row) {
        fail('验证码已失效，请重新获取');
    }
    if ((int)$row['expires_at'] <= time()) {
        fail('验证码已过期，请重新获取');
    }
    if ((int)$row['attempts'] >= 5) {
        fail('验证码错误次数过多，请重新获取');
    }

    if (!hash_equals((string)$row['code'], $code)) {
        $pdo->prepare('UPDATE email_codes SET attempts = attempts + 1 WHERE id = ?')
            ->execute([$row['id']]);
        fail('验证码不正确');
    }

    $pdo->prepare('UPDATE email_codes SET used = 1 WHERE id = ?')
        ->execute([$row['id']]);
}

/**
 * 清理一天前的历史验证码，避免表无限增长
 */
function prune_codes(PDO $pdo): void
{
    $pdo->prepare('DELETE FROM email_codes WHERE created_at < ?')
        ->execute([time() - 86400]);
}
