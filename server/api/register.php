<?php
/**
 * POST /api/register.php
 * 请求体：{ "username", "email", "password", "code" }
 * 返回：{ token, user }
 */

declare(strict_types=1);

require_once __DIR__ . '/../helpers.php';

require_method('POST');

try {
    $data = body();

    $username = assert_username(str_field($data, 'username'));
    $email = assert_email(str_field($data, 'email'));
    $password = assert_password((string)($data['password'] ?? ''));
    $code = assert_code(str_field($data, 'code'));

    $pdo = db();

    // 邮箱 / 用户名占用检查
    $stmt = $pdo->prepare('SELECT username, email FROM users WHERE username = ? OR email = ? LIMIT 1');
    $stmt->execute([$username, $email]);
    $conflict = $stmt->fetch();
    if ($conflict) {
        if (strcasecmp((string)$conflict['email'], $email) === 0) {
            fail('该邮箱已注册，请直接登录');
        }
        fail('该用户名已被占用，请更换一个');
    }

    // 校验并消费验证码（register 场景）
    consume_code($pdo, $email, 'register', $code);

    $now = time();
    $hash = password_hash($password, PASSWORD_DEFAULT);

    try {
        $pdo->beginTransaction();

        $pdo->prepare(
            'INSERT INTO users (username, email, password_hash, avatar, created_at, updated_at)
             VALUES (?, ?, ?, NULL, ?, ?)'
        )->execute([$username, $email, $hash, $now, $now]);

        $userId = (int)$pdo->lastInsertId();
        $token = issue_token($pdo, $userId);

        $stmt = $pdo->prepare('SELECT * FROM users WHERE id = ? LIMIT 1');
        $stmt->execute([$userId]);
        $user = $stmt->fetch();

        $pdo->commit();
    } catch (PDOException $e) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        // 并发注册时唯一索引冲突
        if ($e->getCode() === '23000') {
            fail('用户名或邮箱已被注册');
        }
        throw $e;
    }

    ok('注册成功', [
        'token' => $token,
        'user'  => public_user($user),
    ]);
} catch (Throwable $e) {
    error_log('[JayMusic] register error: ' . $e->getMessage());
    fail('服务开小差了，请稍后重试', 500);
}
