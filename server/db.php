<?php
/**
 * 数据库连接（PDO 单例）
 * 所有接口统一通过 db() 获取连接，避免重复建连。
 */

require_once __DIR__ . '/config.php';

/**
 * @return PDO
 */
function db(): PDO
{
    static $pdo = null;
    if ($pdo instanceof PDO) {
        return $pdo;
    }

    $dsn = sprintf(
        'mysql:host=%s;port=%d;dbname=%s;charset=utf8mb4',
        DB_HOST,
        DB_PORT,
        DB_NAME
    );

    try {
        $pdo = new PDO($dsn, DB_USER, DB_PASS, [
            PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES   => false,
            PDO::ATTR_TIMEOUT            => 5,
        ]);
        // 统一使用东八区时间语义（业务时间均以 Unix 秒存储）
        $pdo->exec("SET time_zone = '+08:00'");
    } catch (PDOException $e) {
        error_log('[JayMusic] DB connect failed: ' . $e->getMessage());
        throw new RuntimeException('数据库连接失败');
    }

    return $pdo;
}
