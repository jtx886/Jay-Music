<?php
/**
 * GET  /api/history.php  -> { records: [...] }
 * POST /api/history.php  { action: "add" | "clear", songId, songName, ... }
 * 需要 Authorization: Bearer {token}
 * 每个用户按 HISTORY_LIMIT 自动裁剪最早记录。
 */

declare(strict_types=1);

require_once __DIR__ . '/../helpers.php';

$user = require_auth();
$userId = (int)$user['id'];

try {
    $method = strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET');

    if ($method === 'GET') {
        $stmt = db()->prepare(
            'SELECT * FROM history WHERE user_id = ? ORDER BY played_at DESC, id DESC'
        );
        $stmt->execute([$userId]);

        $records = array_map('song_payload', $stmt->fetchAll());
        ok('获取成功', ['records' => $records]);
    }

    if ($method !== 'POST') {
        respond(405, '请求方式不被允许', null, 405);
    }

    $data = body();
    $action = str_field($data, 'action');
    $pdo = db();

    if ($action === 'clear') {
        $pdo->prepare('DELETE FROM history WHERE user_id = ?')->execute([$userId]);
        ok('听歌记录已清空');
    }

    if ($action !== 'add') {
        fail('操作类型不正确');
    }

    $songId = clip(str_field($data, 'songId'), 64);
    if ($songId === '') {
        fail('缺少歌曲 ID');
    }

    $now = time();

    $pdo->prepare(
        'INSERT INTO history
            (user_id, song_id, song_name, artist, album, cover_url, source, duration, played_at)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
         ON DUPLICATE KEY UPDATE
            song_name = VALUES(song_name),
            artist    = VALUES(artist),
            album     = VALUES(album),
            cover_url = VALUES(cover_url),
            source    = VALUES(source),
            duration  = VALUES(duration),
            played_at = VALUES(played_at)'
    )->execute([
        $userId,
        $songId,
        clip(str_field($data, 'songName', '未知歌曲'), 255),
        clip(str_field($data, 'artist'), 255),
        clip(str_field($data, 'album'), 255),
        clip(str_field($data, 'coverUrl'), 500),
        clip(str_field($data, 'source'), 20),
        max(0, int_field($data, 'duration')),
        $now,
    ]);

    // 仅保留最近 HISTORY_LIMIT 条（MySQL 不允许直接在 IN 子查询中使用 LIMIT）
    $limit = (int)HISTORY_LIMIT;
    $pdo->prepare(
        "DELETE FROM history
         WHERE user_id = ? AND id NOT IN (
             SELECT id FROM (
                 SELECT id FROM history
                 WHERE user_id = ?
                 ORDER BY played_at DESC, id DESC
                 LIMIT {$limit}
             ) keep_rows
         )"
    )->execute([$userId, $userId]);

    ok('已记录');
} catch (Throwable $e) {
    error_log('[JayMusic] history error: ' . $e->getMessage());
    fail('服务开小差了，请稍后重试', 500);
}
