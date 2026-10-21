<?php
/**
 * Jay Music 后端配置
 * 仅后端使用，Android 客户端不执行任何邮件发送逻辑。
 */

// ==================== 数据库 ====================
define('DB_HOST', '127.0.0.1');
define('DB_PORT', 3306);
define('DB_NAME', 'jay_music');
define('DB_USER', 'root');
define('DB_PASS', '');

// ==================== SMTP 邮件 ====================
define('SMTP_HOST', 'smtp.163.com');
define('SMTP_PORT', 465);
define('SMTP_USER', 'jtxnb886@163.com');
define('SMTP_PASS', 'FLLRDtadYAfGXp9Y');
define('SMTP_FROM', 'jtxnb886@163.com');
define('SMTP_FROM_NAME', 'Jay Music');

// ==================== 业务参数 ====================
define('APP_TIMEZONE', 'Asia/Shanghai');
define('CODE_TTL', 600);          // 验证码有效期：10 分钟
define('CODE_COOLDOWN', 60);      // 同一邮箱发送间隔：60 秒
define('TOKEN_TTL', 30 * 86400);  // 登录令牌有效期：30 天
define('HISTORY_LIMIT', 500);     // 每个用户最多保留的播放记录
