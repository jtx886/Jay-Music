<?php
/**
 * GET /api/user.php
 * 需要 Authorization: Bearer {token}
 * 返回当前登录用户信息。
 */

declare(strict_types=1);

require_once __DIR__ . '/../helpers.php';

require_method('GET');

try {
    $user = require_auth();
    ok('获取成功', public_user($user));
} catch (Throwable $e) {
    error_log('[JayMusic] user error: ' . $e->getMessage());
    fail('服务开小差了，请稍后重试', 500);
}
