<?php
/**
 * POST /api/login.php
 * 请求体：{ "username": "用户名或邮箱", "password": "密码" }
 * 返回：{ token, user }
 */

declare(strict_types=1);

require_once __DIR__ . '/../helpers.php';

require_method('POST');

try {
    $data = body();

    $account = str_field($data, 'username');
    $password = (string)($data['password'] ?? '');

    if ($account === '' || $password === '') {
        fail('请输入用户名和密码');
    }
    if (strlen($account) > 190) {
        fail('账号信息不正确');
    }

    $pdo = db();

    // 同时支持用户名与邮箱登录
    $isEmail = str_contains($account, '@');
    $normalizedEmail = strtolower($account);
    $stmt = $pdo->prepare(
        'SELECT * FROM users WHERE username = ? OR email = ? LIMIT 1'
    );
    $stmt->execute([$account, $normalizedEmail]);
    $user = $stmt->fetch();

    if (!$user || !password_verify($password, (string)$user['password_hash'])) {
        fail('用户名或密码错误');
    }

    // 密码算法升级时顺带刷新哈希
    if (password_needs_rehash((string)$user['password_hash'], PASSWORD_DEFAULT)) {
        $pdo->prepare('UPDATE users SET password_hash = ?, updated_at = ? WHERE id = ?')
            ->execute([
                password_hash($password, PASSWORD_DEFAULT),
                time(),
                (int)$user['id'],
            ]);
    }

    $token = issue_token($pdo, (int)$user['id']);

    ok('登录成功', [
        'token' => $token,
        'user'  => public_user($user),
    ]);
} catch (Throwable $e) {
    error_log('[JayMusic] login error: ' . $e->getMessage());
    fail('服务开小差了，请稍后重试', 500);
}
