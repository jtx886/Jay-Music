package com.jay.music.util

import android.util.Patterns
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 表单校验 */
object Validators {

    fun emailError(email: String): String? = when {
        email.isBlank() -> "请输入邮箱地址"
        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "邮箱格式不正确"
        else -> null
    }

    fun usernameError(username: String): String? = when {
        username.isBlank() -> "请输入用户名"
        username.trim().length < 2 -> "用户名至少 2 个字符"
        username.trim().length > 20 -> "用户名最多 20 个字符"
        else -> null
    }

    fun passwordError(password: String): String? = when {
        password.isEmpty() -> "请输入密码"
        password.length < 6 -> "密码长度至少 6 位"
        else -> null
    }

    fun confirmPasswordError(password: String, confirm: String): String? = when {
        confirm.isEmpty() -> "请再次输入密码"
        confirm != password -> "两次输入的密码不一致"
        else -> null
    }

    fun codeError(code: String): String? = when {
        code.isBlank() -> "请输入验证码"
        !code.matches(Regex("\\d{6}")) -> "验证码为 6 位数字"
        else -> null
    }
}

/** 时间格式化工具 */
object TimeFormat {

    fun duration(ms: Long): String {
        if (ms <= 0) return "00:00"
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    fun timestamp(ms: Long): String {
        if (ms <= 0) return ""
        val now = System.currentTimeMillis()
        val diff = now - ms
        return when {
            diff < 60_000L -> "刚刚"
            diff < 3_600_000L -> "${diff / 60_000L} 分钟前"
            diff < 86_400_000L -> "${diff / 3_600_000L} 小时前"
            else -> SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(ms))
        }
    }
}
