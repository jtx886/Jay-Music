package com.jay.music.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jay.music.core.ServiceLocator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 鉴权页面 UI 状态 */
data class AuthUiState(
    val loading: Boolean = false,
    val sendingCode: Boolean = false,
    val countdown: Int = 0
)

/** 鉴权页面一次性事件 */
sealed interface AuthEvent {
    data class Message(val text: String) : AuthEvent
    data object Registered : AuthEvent
    data object PasswordReset : AuthEvent
}

class AuthViewModel : ViewModel() {

    private val repository = ServiceLocator.authRepository

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 6)
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    private var countdownJob: Job? = null

    /** 发送邮箱验证码；注册与忘记密码复用同一接口 */
    fun sendCode(email: String, scene: String) {
        if (_uiState.value.sendingCode || _uiState.value.countdown > 0) return
        viewModelScope.launch {
            _uiState.update { it.copy(sendingCode = true) }
            val result = repository.sendCode(email.trim(), scene)
            _uiState.update { it.copy(sendingCode = false) }
            if (result.success) {
                startCountdown(60)
                _events.emit(AuthEvent.Message("验证码已发送至 ${email.trim()}，请查收邮件"))
            } else {
                _events.emit(AuthEvent.Message(result.message))
            }
        }
    }

    fun login(username: String, password: String) {
        if (_uiState.value.loading) return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val result = repository.login(username.trim(), password)
            _uiState.update { it.copy(loading = false) }
            if (!result.success) {
                _events.emit(AuthEvent.Message(result.message))
            }
            // 登录成功后 SessionManager 状态变化，根导航会自动切换到主界面
        }
    }

    fun register(
        username: String,
        email: String,
        password: String,
        code: String
    ) {
        if (_uiState.value.loading) return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val result = repository.register(username.trim(), email.trim(), password, code.trim())
            _uiState.update { it.copy(loading = false) }
            if (result.success) {
                _events.emit(AuthEvent.Message("注册成功，请登录"))
                _events.emit(AuthEvent.Registered)
            } else {
                _events.emit(AuthEvent.Message(result.message))
            }
        }
    }

    fun resetPassword(email: String, code: String, newPassword: String) {
        if (_uiState.value.loading) return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val result = repository.resetPassword(email.trim(), code.trim(), newPassword)
            _uiState.update { it.copy(loading = false) }
            if (result.success) {
                _events.emit(AuthEvent.Message("密码重置成功，请使用新密码登录"))
                _events.emit(AuthEvent.PasswordReset)
            } else {
                _events.emit(AuthEvent.Message(result.message))
            }
        }
    }

    private fun startCountdown(seconds: Int) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var remain = seconds
            while (remain > 0) {
                _uiState.update { it.copy(countdown = remain) }
                delay(1_000)
                remain -= 1
            }
            _uiState.update { it.copy(countdown = 0) }
        }
    }

    override fun onCleared() {
        countdownJob?.cancel()
        super.onCleared()
    }
}
