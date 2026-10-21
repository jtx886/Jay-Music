<?php
/**
 * GET  /api/favorites.php  -> { songs: [...] }
 * POST /api/favorites.php  { action: "add" | "remove", songId, songName, ... }
 * 需要 Authorization: Bearer {token}
 */

declare(strict_types=1);

require_once __DIR__ . '/../helpers.php';

$user = require_auth();
$userId = (int)$user['id'];

try {
    $method = strtoupper($_SERVER['REQUEST_METHOD'] ?? 'GET');

    if ($method === 'GET') {
        $stmt = db()->prepare(
            'SELECT * FROM favorites WHERE user_id = ? ORDER BY created_at DESC, id DESC'
        );
        $stmt->execute([$userId]);

        $songs = array_map('song_payload', $stmt->fetchAll());
        ok('获取成功', ['songs' => $songs]);
    }

    if ($method !== 'POST') {
        respond(405, '请求方式不被允许', null, 405);
    }

    $data = body();
    $action = str_field($data, 'action');
    $songId = clip(str_field($data, 'songId'), 64);

    if ($songId === '') {
        fail('缺少歌曲 ID');
    }

    $pdo = db();

    if ($action === 'remove') {
        $pdo->prepare('DELETE FROM favorites WHERE user_id = ? AND song_id = ?')
            ->execute([$userId, $songId]);
        ok('已取消收藏');
    }

    if ($action !== 'add') {
        fail('操作类型不正确');
    }

    $stmt = $pdo->prepare(
        'INSERT INTO favorites
            (user_id, song_id, song_name, artist, album, cover_url, source, duration, created_at)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
         ON DUPLICATE KEY UPDATE
            song_name = VALUES(song_name),
            artist    = VALUES(artist),
            album     = VALUES(album),
            cover_url = VALUES(cover_url),
            source    = VALUES(source),
            duration  = VALUES(duration)'
    );

    $stmt->execute([
        $userId,
        $songId,
        clip(str_field($data, 'songName', '未知歌曲'), 255),
        clip(str_field($data, 'artist'), 255),
        clip(str_field($data, 'album'), 255),
        clip(str_field($data, 'coverUrl'), 500),
        clip(str_field($data, 'source'), 20),
        max(0, int_field($data, 'duration')),
        time(),
    ]);

    ok('已收藏');
} catch (Throwable $e) {
    error_log('[JayMusic] favorites error: ' . $e->getMessage());
    fail('服务开小差了，请稍后重试', 500);
}
