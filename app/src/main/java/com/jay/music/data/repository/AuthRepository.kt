package com.jay.music.data.repository

import com.jay.music.core.SessionManager
import com.jay.music.data.local.LocalAccountStore
import com.jay.music.data.model.ApiResult
import com.jay.music.data.model.User
import java.util.UUID

/**
 * 本机账号仓库（无后端）：
 * 注册、登录全部在本机完成，不依赖网络与邮件验证码。
 */
class AuthRepository(
    private val accountStore: LocalAccountStore,
    private val sessionManager: SessionManager
) {

    /** 本机是否已注册过账号 */
    fun hasAccount(): Boolean = accountStore.hasAccount()

    /** 本机账号用户名（用于登录页提示） */
    fun localUsername(): String? = accountStore.username()

    /**
     * 注册：仅保存用户名与密码哈希到本机加密存储。
     */
    suspend fun register(username: String, password: String): ApiResult<Unit> {
        if (accountStore.hasAccount()) {
            return ApiResult(false, "本机已有账号，请直接登录")
        }
        val saved = runCatching { accountStore.register(username, password) }
            .getOrDefault(false)
        return if (saved) {
            ApiResult(true, "注册成功，请登录")
        } else {
            ApiResult(false, "账号保存失败，请稍后重试")
        }
    }

    /**
     * 登录：校验本机账号后写入加密会话。
     */
    suspend fun login(username: String, password: String): ApiResult<Unit> {
        val storedUsername = accountStore.username()
            ?: return ApiResult(false, "本机还没有账号，请先注册")

        if (!accountStore.verify(username, password)) {
            return ApiResult(false, "用户名或密码错误")
        }

        val user = User(
            id = 1L,
            username = storedUsername,
            email = "",
            avatar = null
        )
        sessionManager.saveSession(UUID.randomUUID().toString(), user)
        return ApiResult(true, "登录成功")
    }

    /**
     * 本地账号信息直接从会话读取，无需网络请求。
     */
    suspend fun fetchUser(): ApiResult<User> {
        val user = sessionManager.user.value
            ?: return ApiResult(false, "登录状态已失效，请重新登录")
        return ApiResult(true, "获取成功", user)
    }

    fun logout() = sessionManager.logout()
}
