package com.jay.music.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jay.music.ui.components.AnimatedAppBackground
import com.jay.music.ui.components.GlassCard
import com.jay.music.ui.components.GradientButton
import com.jay.music.ui.components.ScreenTopBar
import com.jay.music.ui.theme.BrandGreen
import com.jay.music.ui.theme.BrandPurple
import com.jay.music.util.Validators

/** 忘记密码 / 重置密码页 */
@Composable
fun ResetPasswordScreen(
    onBack: () -> Unit,
    onResetSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var email by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var codeError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AuthEvent.Message -> Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
                AuthEvent.PasswordReset -> onResetSuccess()
                AuthEvent.Registered -> Unit
            }
        }
    }

    fun requestCode() {
        val error = Validators.emailError(email)
        emailError = error
        if (error == null) viewModel.sendCode(email, "reset")
    }

    fun submit() {
        emailError = Validators.emailError(email)
        codeError = Validators.codeError(code)
        passwordError = Validators.passwordError(newPassword)
        confirmError = Validators.confirmPasswordError(newPassword, confirmPassword)
        val valid = listOf(emailError, codeError, passwordError, confirmError).all { it == null }
        if (valid) {
            focusManager.clearFocus()
            viewModel.resetPassword(email, code, newPassword)
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
            ScreenTopBar(title = "重置密码", onBack = onBack)
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "找回你的账号",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "验证注册邮箱后即可设置新密码",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(26.dp))

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    AuthTextField(
                        value = email,
                        onValueChange = { email = it; emailError = null },
                        label = "注册邮箱",
                        placeholder = "请输入注册时使用的邮箱",
                        leadingIcon = Icons.Rounded.AlternateEmail,
                        error = emailError,
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
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
                        imeAction = ImeAction.Next,
                        trailingContent = {
                            TextButton(onClick = { requestCode() }, enabled = codeButtonEnabled) {
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
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                    Spacer(Modifier.height(14.dp))

                    AuthTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; passwordError = null; confirmError = null },
                        label = "新密码",
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
                        label = "确认新密码",
                        placeholder = "再次输入新密码",
                        leadingIcon = Icons.Rounded.Lock,
                        error = confirmError,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        visualTransformation = if (showPassword) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        onImeAction = { submit() }
                    )

                    Spacer(Modifier.height(28.dp))

                    GradientButton(
                        text = "重置密码",
                        loading = uiState.loading,
                        onClick = { submit() }
                    )

                    Spacer(Modifier.height(16.dp))

                    AuthLinkRow(
                        prefix = "想起密码了？ ",
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
