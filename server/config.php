<?php
/**
 * Jay Music 后端配置
 * 仅后端使用，Android 客户端不执行任何邮件发送逻辑。
 *
 * 推荐通过环境变量注入敏感信息；未设置时使用下方默认值。
 * 例如：JAY_DB_HOST / JAY_DB_NAME / JAY_DB_USER / JAY_DB_PASS /
 *       JAY_SMTP_HOST / JAY_SMTP_PORT / JAY_SMTP_USER / JAY_SMTP_PASS / JAY_SMTP_FROM
 */

/** 读取环境变量，未设置时回退到默认值 */
function env_or(string $key, string $default = ''): string
{
    $value = getenv($key);
    return $value === false || $value === '' ? $default : $value;
}

// ==================== 数据库 ====================
define('DB_HOST', env_or('JAY_DB_HOST', '127.0.0.1'));
define('DB_PORT', (int)env_or('JAY_DB_PORT', '3306'));
define('DB_NAME', env_or('JAY_DB_NAME', 'jay_music'));
define('DB_USER', env_or('JAY_DB_USER', 'root'));
define('DB_PASS', env_or('JAY_DB_PASS', ''));

// ==================== SMTP 邮件 ====================
define('SMTP_HOST', env_or('JAY_SMTP_HOST', 'smtp.163.com'));
define('SMTP_PORT', (int)env_or('JAY_SMTP_PORT', '465'));
define('SMTP_USER', env_or('JAY_SMTP_USER', ''));
define('SMTP_PASS', env_or('JAY_SMTP_PASS', ''));
define('SMTP_FROM', env_or('JAY_SMTP_FROM', SMTP_USER));
define('SMTP_FROM_NAME', env_or('JAY_SMTP_FROM_NAME', 'Jay Music'));

// ==================== 业务参数 ====================
define('APP_TIMEZONE', 'Asia/Shanghai');
define('CODE_TTL', 600);          // 验证码有效期：10 分钟
define('CODE_COOLDOWN', 60);      // 同一邮箱发送间隔：60 秒
define('TOKEN_TTL', 30 * 86400);  // 登录令牌有效期：30 天
define('HISTORY_LIMIT', 500);     // 每个用户最多保留的播放记录
