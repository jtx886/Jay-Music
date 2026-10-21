package com.jay.music.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jay.music.ui.components.isDarkTheme
import com.jay.music.ui.components.shakeOnError
import com.jay.music.ui.theme.BrandGreen
import com.jay.music.ui.theme.BrandMint
import com.jay.music.ui.theme.BrandPurple

/** 品牌 Logo 占位 */
@Composable
fun AuthLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(86.dp)
            .shadow(
                elevation = 26.dp,
                shape = CircleShape,
                ambientColor = BrandGreen.copy(alpha = 0.7f),
                spotColor = BrandPurple.copy(alpha = 0.8f)
            )
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(BrandGreen, BrandMint, BrandPurple))),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.GraphicEq,
            contentDescription = "Jay Music Logo",
            tint = Color(0xFF07120B),
            modifier = Modifier.size(42.dp)
        )
    }
}

/** 渐变主标题 */
@Composable
fun GradientTitle(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: Int = 44
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.displayLarge.copy(
            brush = Brush.linearGradient(listOf(BrandGreen, BrandMint, BrandPurple)),
            fontSize = fontSize.sp
        ),
        textAlign = TextAlign.Center
    )
}

/**
 * 鉴权通用输入框：
 * 左侧标题、占位提示、错误提示、错误抖动。
 */
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: ImageVector? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onImeAction: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shakeOnError(error != null)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            leadingIcon = if (leadingIcon != null) {
                {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else null,
            trailingIcon = trailingContent,
            singleLine = true,
            isError = error != null,
            shape = RoundedCornerShape(18.dp),
            textStyle = MaterialTheme.typography.bodyLarge,
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onNext = { onImeAction() },
                onDone = { onImeAction() },
                onGo = { onImeAction() },
                onSearch = { onImeAction() }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedContainerColor = if (isDarkTheme()) Color.White.copy(alpha = 0.05f)
                else Color.White.copy(alpha = 0.8f),
                unfocusedContainerColor = if (isDarkTheme()) Color.White.copy(alpha = 0.035f)
                else Color.White.copy(alpha = 0.65f),
                errorContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.06f),
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )
        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = error.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 8.dp, top = 6.dp)
            )
        }
    }
}

/** 底部可点击文字链接 */
@Composable
fun AuthLinkRow(
    prefix: String,
    action: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = prefix,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = action,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

/** 间距 */
@Composable
fun AuthSpacer(height: Int) {
    Spacer(modifier = Modifier.height(height.dp))
}
