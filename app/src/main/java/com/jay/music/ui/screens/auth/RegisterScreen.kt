package com.jay.music.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jay.music.ui.components.AnimatedAppBackground
import com.jay.music.ui.components.GlassCard
import com.jay.music.ui.components.GradientButton
import com.jay.music.ui.components.RoundedIconButton
import com.jay.music.ui.components.ScreenTopBar
import com.jay.music.ui.theme.BrandGreen
import com.jay.music.ui.theme.BrandPurple
import com.jay.music.util.Validators

/** 注册页 */
@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onRegistered: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    var usernameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }
    var codeError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AuthEvent.Message -> Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
                AuthEvent.Registered -> onRegistered()
                AuthEvent.PasswordReset -> Unit
            }
        }
    }

    fun requestCode() {
        val error = Validators.emailError(email)
        emailError = error
        if (error == null) viewModel.sendCode(email, "register")
    }

    fun submit() {
        usernameError = Validators.usernameError(username)
        emailError = Validators.emailError(email)
        passwordError = Validators.passwordError(password)
        confirmError = Validators.confirmPasswordError(password, confirmPassword)
        codeError = Validators.codeError(code)
        val valid = listOf(
            usernameError, emailError, passwordError, confirmError, codeError
        ).all { it == null }
        if (valid) {
            focusManager.clearFocus()
            viewModel.register(username, email, password, code)
        }
    }

    val countdown = uiState.countdown
    val codeButtonEnabled = countdown == 0 && !uiState.sendingCode

    AnimatedAppBackground(accentColors = listOf(BrandPurple, BrandGreen)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            ScreenTopBar(title = "创建账号", onBack = onBack)
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "加入 Jay Music",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "开启属于你的音乐宇宙",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(26.dp))

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    AuthTextField(
                        value = username,
                        onValueChange = { username = it; usernameError = null },
                        label = "用户名",
                        placeholder = "2-20 个字符",
                        leadingIcon = Icons.Rounded.Person,
                        error = usernameError,
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                    Spacer(Modifier.height(14.dp))

                    AuthTextField(
                        value = email,
                        onValueChange = { email = it; emailError = null },
                        label = "邮箱地址",
                        placeholder = "用于接收验证码",
                        leadingIcon = Icons.Rounded.AlternateEmail,
                        error = emailError,
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                    Spacer(Modifier.height(14.dp))

                    AuthTextField(
                        value = password,
                        onValueChange = { password = it; passwordError = null; confirmError = null },
                        label = "密码",
                        placeholder = "至少 6 位",
                        leadingIcon = Icons.Rounded.Lock,
                        error = passwordError,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                        visualTransformation = if (showPassword) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        trailingContent = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Rounded.VisibilityOff
                                    else Icons.Rounded.Visibility,
                                    contentDescription = "切换密码可见",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                    Spacer(Modifier.height(14.dp))

                    AuthTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; confirmError = null },
                        label = "确认密码",
                        placeholder = "再次输入密码",
                        leadingIcon = Icons.Rounded.Lock,
                        error = confirmError,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                        visualTransformation = if (showPassword) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                    Spacer(Modifier.height(14.dp))

                    AuthTextField(
                        value = code,
                        onValueChange = {
                            code = it.filter { char -> char.isDigit() }.take(6)
                            codeError = null
                        },
                        label = "邮箱验证码",
                        placeholder = "6 位数字验证码",
                        leadingIcon = Icons.Rounded.Pin,
                        error = codeError,
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                        trailingContent = {
                            TextButton(
                                onClick = { requestCode() },
                                enabled = codeButtonEnabled
                            ) {
                                if (uiState.sendingCode) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Text(
                                        text = if (countdown > 0) "${countdown}s" else "获取验证码",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (codeButtonEnabled) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        onImeAction = { submit() }
                    )

                    Spacer(Modifier.height(28.dp))

                    GradientButton(
                        text = "注 册",
                        loading = uiState.loading,
                        onClick = { submit() }
                    )

                    Spacer(Modifier.height(16.dp))

                    AuthLinkRow(
                        prefix = "已有账号？ ",
                        action = "返回登录",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onBack
                    )
                }
                Spacer(Modifier.height(34.dp))
            }
        }
    }
}
