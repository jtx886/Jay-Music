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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.jay.music.ui.theme.BrandGreen
import com.jay.music.ui.theme.BrandPurple
import com.jay.music.util.Validators

/** 登录页 */
@Composable
fun LoginScreen(
    onGoRegister: () -> Unit,
    onGoReset: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is AuthEvent.Message) {
                Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun submit() {
        usernameError = if (username.isBlank()) "请输入用户名" else null
        passwordError = Validators.passwordError(password)
        if (usernameError == null && passwordError == null) {
            focusManager.clearFocus()
            viewModel.login(username, password)
        }
    }

    AnimatedAppBackground(
        accentColors = listOf(BrandGreen, BrandPurple)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(52.dp))
            AuthLogo()
            Spacer(Modifier.height(22.dp))
            GradientTitle(text = "Jay Music")
            Spacer(Modifier.height(8.dp))
            Text(
                text = "让每一次聆听，都成为一场旅行",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(34.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "欢迎回来",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "登录后同步你的收藏与听歌记录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))

                AuthTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        usernameError = null
                    },
                    label = "用户名",
                    placeholder = "请输入用户名",
                    leadingIcon = Icons.Rounded.Person,
                    error = usernameError,
                    imeAction = ImeAction.Next,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
                )
                Spacer(Modifier.height(16.dp))

                AuthTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = null
                    },
                    label = "密码",
                    placeholder = "请输入密码",
                    leadingIcon = Icons.Rounded.Lock,
                    error = passwordError,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    visualTransformation = if (showPassword) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingContent = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Rounded.VisibilityOff
                                else Icons.Rounded.Visibility,
                                contentDescription = if (showPassword) "隐藏密码" else "显示密码",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onImeAction = { submit() }
                )

                Spacer(Modifier.height(28.dp))

                GradientButton(
                    text = "登 录",
                    loading = uiState.loading,
                    onClick = { submit() }
                )

                Spacer(Modifier.height(16.dp))

                AuthLinkRow(
                    prefix = "没有账号？ ",
                    action = "立即注册",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onGoRegister
                )
                Spacer(Modifier.height(10.dp))
                AuthLinkRow(
                    prefix = "忘记密码？ ",
                    action = "重置密码",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onGoReset
                )
            }

            Spacer(Modifier.height(28.dp))
            Text(
                text = "登录即代表你同意《用户协议》与《隐私政策》",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}
