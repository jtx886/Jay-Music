<?php
/**
 * 健康检查：GET /
 * 用于确认 PHP 环境与数据库连接是否正常。
 */

declare(strict_types=1);

require_once __DIR__ . '/helpers.php';

try {
    db()->query('SELECT 1');
    ok('Jay Music API 运行中', [
        'version'    => '1.0.0',
        'phpVersion' => PHP_VERSION,
        'time'       => date('Y-m-d H:i:s'),
    ]);
} catch (Throwable $e) {
    fail('数据库连接失败，请检查 config.php 配置', 500);
}
