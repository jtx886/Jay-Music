package com.jay.music.core

import com.jay.music.data.local.SecureStorage
import com.jay.music.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 全局登录会话：内存中维护 StateFlow，持久化交给 SecureStorage。
 * 所有敏感信息（token / user）均加密落盘。
 */
class SessionManager(private val storage: SecureStorage) {

    private val _token = MutableStateFlow(storage.token)
    val token: StateFlow<String?> = _token.asStateFlow()

    private val _user = MutableStateFlow(storage.user)
    val user: StateFlow<User?> = _user.asStateFlow()

    val isLoggedIn: Boolean get() = !_token.value.isNullOrBlank()

    fun saveSession(token: String, user: User) {
        storage.saveSession(token, user)
        _token.value = token
        _user.value = user
    }

    fun updateUser(user: User) {
        storage.user = user
        _user.value = user
    }

    fun logout() {
        storage.clear()
        _token.value = null
        _user.value = null
    }
}
