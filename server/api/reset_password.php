<?php
/**
 * POST /api/reset_password.php
 * 请求体：{ "email", "code", "newPassword" }
 * 重置成功后该用户所有已签发的令牌都会失效，需要重新登录。
 */

declare(strict_types=1);

require_once __DIR__ . '/../helpers.php';

require_method('POST');

try {
    $data = body();

    $email = assert_email(str_field($data, 'email'));
    $code = assert_code(str_field($data, 'code'));
    $newPassword = assert_password((string)($data['newPassword'] ?? ''));

    $pdo = db();

    $stmt = $pdo->prepare('SELECT id FROM users WHERE email = ? LIMIT 1');
    $stmt->execute([$email]);
    $userId = $stmt->fetchColumn();

    if ($userId === false) {
        fail('该邮箱尚未注册');
    }

    // 校验并消费 reset 场景验证码
    consume_code($pdo, $email, 'reset', $code);

    $pdo->beginTransaction();
    try {
        $pdo->prepare('UPDATE users SET password_hash = ?, updated_at = ? WHERE id = ?')
            ->execute([password_hash($newPassword, PASSWORD_DEFAULT), time(), (int)$userId]);

        // 重置密码后强制所有设备重新登录
        $pdo->prepare('DELETE FROM auth_tokens WHERE user_id = ?')
            ->execute([(int)$userId]);

        $pdo->commit();
    } catch (Throwable $e) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        throw $e;
    }

    ok('密码重置成功，请使用新密码登录');
} catch (Throwable $e) {
    error_log('[JayMusic] reset_password error: ' . $e->getMessage());
    fail('服务开小差了，请稍后重试', 500);
}
