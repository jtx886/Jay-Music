-- ============================================================
-- Jay Music 后端数据库结构
-- MySQL 5.7+ / MariaDB 10.3+
-- 导入方式：mysql -u root -p < sql/schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS `jay_music`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `jay_music`;

-- ------------------------- 用户表 -------------------------
CREATE TABLE IF NOT EXISTS `users` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `username`      VARCHAR(20)     NOT NULL COMMENT '用户名',
    `email`         VARCHAR(190)    NOT NULL COMMENT '邮箱',
    `password_hash` VARCHAR(255)    NOT NULL COMMENT '密码哈希（password_hash）',
    `avatar`        VARCHAR(255)    DEFAULT NULL COMMENT '头像地址',
    `created_at`    BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建时间（Unix 秒）',
    `updated_at`    BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '更新时间（Unix 秒）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_users_username` (`username`),
    UNIQUE KEY `uk_users_email` (`email`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表';

-- ------------------------- 邮箱验证码表 -------------------------
CREATE TABLE IF NOT EXISTS `email_codes` (
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `email`      VARCHAR(190)    NOT NULL COMMENT '接收邮箱',
    `code`       CHAR(6)         NOT NULL COMMENT '6 位数字验证码',
    `scene`      VARCHAR(16)     NOT NULL COMMENT 'register / reset',
    `attempts`   TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已尝试次数',
    `used`       TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否已使用',
    `expires_at` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '过期时间（Unix 秒）',
    `created_at` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '创建时间（Unix 秒）',
    PRIMARY KEY (`id`),
    KEY `idx_codes_email_scene` (`email`, `scene`, `id`),
    KEY `idx_codes_created` (`created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '邮箱验证码';

-- ------------------------- 登录令牌表 -------------------------
CREATE TABLE IF NOT EXISTS `auth_tokens` (
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
    `token`      VARCHAR(64)     NOT NULL COMMENT '64 位十六进制令牌',
    `expires_at` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '过期时间（Unix 秒）',
    `created_at` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '签发时间（Unix 秒）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tokens_token` (`token`),
    KEY `idx_tokens_user` (`user_id`),
    KEY `idx_tokens_expires` (`expires_at`),
    CONSTRAINT `fk_tokens_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '登录令牌';

-- ------------------------- 收藏表 -------------------------
CREATE TABLE IF NOT EXISTS `favorites` (
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
    `song_id`    VARCHAR(64)     NOT NULL COMMENT '歌曲 ID（第三方平台）',
    `song_name`  VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '歌曲名',
    `artist`     VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '歌手',
    `album`      VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '专辑',
    `cover_url`  VARCHAR(500)    NOT NULL DEFAULT '' COMMENT '封面地址',
    `source`     VARCHAR(20)     NOT NULL DEFAULT '' COMMENT '音源：NETEASE / KUWO',
    `duration`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '时长（毫秒）',
    `created_at` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '收藏时间（Unix 秒）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_favorites_user_song` (`user_id`, `song_id`),
    KEY `idx_favorites_user_created` (`user_id`, `created_at`),
    CONSTRAINT `fk_favorites_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '我的收藏';

-- ------------------------- 播放历史表 -------------------------
CREATE TABLE IF NOT EXISTS `history` (
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
    `song_id`    VARCHAR(64)     NOT NULL COMMENT '歌曲 ID（第三方平台）',
    `song_name`  VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '歌曲名',
    `artist`     VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '歌手',
    `album`      VARCHAR(255)    NOT NULL DEFAULT '' COMMENT '专辑',
    `cover_url`  VARCHAR(500)    NOT NULL DEFAULT '' COMMENT '封面地址',
    `source`     VARCHAR(20)     NOT NULL DEFAULT '' COMMENT '音源：NETEASE / KUWO',
    `duration`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '时长（毫秒）',
    `played_at`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '最近播放时间（Unix 秒）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_history_user_song` (`user_id`, `song_id`),
    KEY `idx_history_user_played` (`user_id`, `played_at`),
    CONSTRAINT `fk_history_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '播放历史';
