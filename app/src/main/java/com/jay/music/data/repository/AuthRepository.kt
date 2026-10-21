package com.jay.music.data.repository

import com.jay.music.core.SessionManager
import com.jay.music.data.model.ApiResult
import com.jay.music.data.model.AuthSession
import com.jay.music.data.model.User
import com.jay.music.data.remote.JayMusicApiService
import com.jay.music.data.remote.dto.ApiResponse
import com.jay.music.data.remote.dto.LoginRequest
import com.jay.music.data.remote.dto.RegisterRequest
import com.jay.music.data.remote.dto.ResetPasswordRequest
import com.jay.music.data.remote.dto.SendCodeRequest
import com.jay.music.data.remote.dto.UserDto
import java.io.IOException

/**
 * 账号仓库：验证码、注册、登录、重置密码、用户信息。
 */
class AuthRepository(
    private val api: JayMusicApiService,
    private val sessionManager: SessionManager
) {

    suspend fun sendCode(email: String, scene: String): ApiResult<Unit> =
        call { api.sendCode(SendCodeRequest(email, scene)) }
            .mapData { }

    /**
     * 注册成功后不直接建立登录态：业务要求注册完成自动跳回登录页。
     * 后端仍会返回 token 与基础信息，这里只做校验并回传给界面。
     */
    suspend fun register(
        username: String,
        email: String,
        password: String,
        code: String
    ): ApiResult<AuthSession> {
        val result = call { api.register(RegisterRequest(username, email, password, code)) }
        if (result.success) {
            val data = result.data
            val user = data?.user?.toUser()
            val token = data?.token
            if (token.isNullOrBlank() || user == null) {
                return ApiResult(false, "注册信息解析失败，请稍后重试")
            }
            return ApiResult(true, result.message, AuthSession(token, user))
        }
        return ApiResult(false, result.message)
    }

    suspend fun login(username: String, password: String): ApiResult<AuthSession> {
        val result = call { api.login(LoginRequest(username, password)) }
        if (result.success) {
            val data = result.data
            val user = data?.user?.toUser()
            val token = data?.token
            if (token.isNullOrBlank() || user == null) {
                return ApiResult(false, "登录信息解析失败，请稍后重试")
            }
            sessionManager.saveSession(token, user)
            return ApiResult(true, result.message)
        }
        return ApiResult(false, result.message)
    }

    suspend fun resetPassword(email: String, code: String, newPassword: String): ApiResult<Unit> =
        call { api.resetPassword(ResetPasswordRequest(email, code, newPassword)) }
            .mapData { }

    suspend fun fetchUser(): ApiResult<User> {
        val result = call { api.getUser() }
        if (result.success) {
            val user = result.data?.toUser()
            if (user != null) {
                sessionManager.updateUser(user)
                return ApiResult(true, result.message, user)
            }
        }
        return ApiResult(false, result.message)
    }

    fun logout() = sessionManager.logout()

    // ------------------------- 工具 -------------------------

    private fun UserDto.toUser() = User(id = id, username = username, email = email, avatar = avatar)

    private suspend fun <T> call(block: suspend () -> ApiResponse<T>): ApiResult<T> {
        return try {
            val response = block()
            if (response.isSuccess) {
                ApiResult(true, response.message ?: "操作成功", response.data)
            } else {
                ApiResult(false, response.message ?: "请求失败，请稍后重试")
            }
        } catch (e: IOException) {
            ApiResult(false, "网络连接失败，请检查网络后重试")
        } catch (e: Exception) {
            ApiResult(false, e.message ?: "服务异常，请稍后重试")
        }
    }

    private fun <T, R> ApiResult<T>.mapData(transform: (T?) -> R): ApiResult<R> =
        ApiResult(success = success, message = message, data = transform(data))
}
