package com.jay.music.ui.navigation

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jay.music.core.ServiceLocator
import com.jay.music.ui.components.AnimatedAppBackground
import com.jay.music.ui.components.MiniPlayerHost
import com.jay.music.ui.components.isDarkTheme
import com.jay.music.ui.screens.auth.LoginScreen
import com.jay.music.ui.screens.auth.RegisterScreen
import com.jay.music.ui.screens.home.HomeScreen
import com.jay.music.ui.screens.library.FavoritesScreen
import com.jay.music.ui.screens.library.HistoryScreen
import com.jay.music.ui.screens.library.LibraryViewModel
import com.jay.music.ui.screens.player.PlayerScreen
import com.jay.music.ui.screens.profile.ProfileScreen
import com.jay.music.ui.screens.search.SearchScreen
import com.jay.music.ui.theme.BrandGreen
import com.jay.music.ui.theme.BrandMint
import com.jay.music.ui.theme.BrandPurple

/** 鉴权流程内部页面 */
private enum class AuthRoute { LOGIN, REGISTER }

/** 主流程路由 */
private enum class RootRoute { MAIN, SEARCH, FAVORITES, HISTORY, PLAYER }

/**
 * 应用根导航：
 * 1. 根据本地登录态判断，未登录只能停在登录/注册页面
 * 2. 登录后进入主界面（首页 / 我的双 Tab）
 * 3. 搜索、收藏、历史、全屏播放器作为独立路由覆盖在主界面之上
 * 4. 全局 SharedTransitionLayout，支撑迷你播放器 -> 全屏播放器的共享元素转场
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun JayMusicRoot() {
    val token by ServiceLocator.sessionManager.token.collectAsStateWithLifecycle()
    val libraryViewModel: LibraryViewModel = viewModel()
    val context = LocalContext.current

    // 全局播放提示（解析失败 / 音源降级 / 服务异常等）
    LaunchedEffect(Unit) {
        ServiceLocator.playbackController.events.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // 进入主界面后刷新一次用户信息（使用加密 token 鉴权）
    LaunchedEffect(token) {
        if (!token.isNullOrBlank()) {
            runCatching { ServiceLocator.authRepository.fetchUser() }
        }
    }

    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        val sharedScope = this
        AnimatedContent(
            targetState = !token.isNullOrBlank(),
            transitionSpec = { fadeIn(tween(420)) togetherWith fadeOut(tween(260)) },
            label = "authGate"
        ) { loggedIn ->
            if (loggedIn) {
                MainApp(libraryViewModel = libraryViewModel, sharedScope = sharedScope)
            } else {
                AuthFlow()
            }
        }
    }
}

/** 登录 / 注册 双页路由 */
@Composable
private fun AuthFlow() {
    var route by rememberSaveable { mutableStateOf(AuthRoute.LOGIN) }

    AnimatedContent(
        targetState = route,
        transitionSpec = {
            if (targetState.ordinal > initialState.ordinal) {
                (fadeIn(tween(320)) + slideInHorizontally(tween(360)) { it / 10 }) togetherWith
                    (fadeOut(tween(180)) + slideOutHorizontally(tween(220)) { -it / 14 })
            } else {
                (fadeIn(tween(320)) + slideInHorizontally(tween(360)) { -it / 10 }) togetherWith
                    (fadeOut(tween(180)) + slideOutHorizontally(tween(220)) { it / 14 })
            }
        },
        label = "authFlow"
    ) { current ->
        when (current) {
            AuthRoute.LOGIN -> LoginScreen(
                onGoRegister = { route = AuthRoute.REGISTER }
            )
            AuthRoute.REGISTER -> RegisterScreen(
                onBack = { route = AuthRoute.LOGIN },
                onRegistered = { route = AuthRoute.LOGIN }
            )
        }
    }
}

/** 登录后的主应用 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun MainApp(
    libraryViewModel: LibraryViewModel,
    sharedScope: SharedTransitionScope
) {
    var route by rememberSaveable { mutableStateOf(RootRoute.MAIN) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    // 记录进入全屏播放器前的页面，退出播放器时回到原页面
    var playerReturnRoute by rememberSaveable { mutableStateOf(RootRoute.MAIN) }

    BackHandler(enabled = route != RootRoute.MAIN) {
        route = if (route == RootRoute.PLAYER) playerReturnRoute else RootRoute.MAIN
    }

    AnimatedAppBackground(accentColors = listOf(BrandGreen, BrandPurple)) {
        AnimatedContent(
            targetState = route,
            transitionSpec = { rootTransition(initialState, targetState) },
            modifier = Modifier.fillMaxSize(),
            label = "rootRoute"
        ) { current ->
            val visibilityScope = this
            when (current) {
                RootRoute.MAIN -> MainTabs(
                    sharedScope = sharedScope,
                    animatedVisibilityScope = visibilityScope,
                    libraryViewModel = libraryViewModel,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    onOpenSearch = { route = RootRoute.SEARCH },
                    onOpenFavorites = { route = RootRoute.FAVORITES },
                    onOpenHistory = { route = RootRoute.HISTORY },
                    onOpenPlayer = {
                        playerReturnRoute = RootRoute.MAIN
                        route = RootRoute.PLAYER
                    }
                )
                RootRoute.SEARCH -> SearchScreen(
                    libraryViewModel = libraryViewModel,
                    sharedTransitionScope = sharedScope,
                    animatedVisibilityScope = visibilityScope,
                    onBack = { route = RootRoute.MAIN },
                    onOpenPlayer = {
                        playerReturnRoute = RootRoute.SEARCH
                        route = RootRoute.PLAYER
                    }
                )
                RootRoute.FAVORITES -> FavoritesScreen(
                    libraryViewModel = libraryViewModel,
                    sharedTransitionScope = sharedScope,
                    animatedVisibilityScope = visibilityScope,
                    onBack = { route = RootRoute.MAIN },
                    onOpenPlayer = {
                        playerReturnRoute = RootRoute.FAVORITES
                        route = RootRoute.PLAYER
                    }
                )
                RootRoute.HISTORY -> HistoryScreen(
                    libraryViewModel = libraryViewModel,
                    sharedTransitionScope = sharedScope,
                    animatedVisibilityScope = visibilityScope,
                    onBack = { route = RootRoute.MAIN },
                    onOpenPlayer = {
                        playerReturnRoute = RootRoute.HISTORY
                        route = RootRoute.PLAYER
                    }
                )
                RootRoute.PLAYER -> PlayerScreen(
                    sharedTransitionScope = sharedScope,
                    animatedVisibilityScope = visibilityScope,
                    libraryViewModel = libraryViewModel,
                    onBack = { route = playerReturnRoute }
                )
            }
        }
    }
}

/** 主界面：双 Tab 内容 + 底部常驻迷你播放器 + 底部导航栏 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun MainTabs(
    sharedScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    libraryViewModel: LibraryViewModel,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    val playerState by ServiceLocator.playbackController.state.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        // Tab 切换：缩放 + 淡入动画
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                (fadeIn(tween(320)) + scaleIn(tween(320), initialScale = 0.96f)) togetherWith
                    (fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 1.03f))
            },
            label = "tabContent"
        ) { tab ->
            when (tab) {
                0 -> HomeScreen(
                    libraryViewModel = libraryViewModel,
                    playerState = playerState,
                    onOpenSearch = onOpenSearch
                )
                else -> ProfileScreen(
                    libraryViewModel = libraryViewModel,
                    onOpenFavorites = onOpenFavorites,
                    onOpenHistory = onOpenHistory
                )
            }
        }

        // 底部：迷你播放器 + 导航栏（含渐变遮罩，保证文字可读）
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.92f)
                            )
                        )
                    )
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                MiniPlayerHost(
                    sharedTransitionScope = sharedScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onOpenPlayer = onOpenPlayer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
                BottomTabBar(selectedTab = selectedTab, onSelect = onTabSelected)
            }
        }
    }
}

/** 底部双 Tab 导航栏 */
@Composable
private fun BottomTabBar(
    selectedTab: Int,
    onSelect: (Int) -> Unit
) {
    val dark = isDarkTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 56.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(if (dark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.88f))
            .border(
                1.dp,
                if (dark) Color.White.copy(alpha = 0.10f) else Color.White,
                RoundedCornerShape(30.dp)
            )
            .padding(vertical = 6.dp)
    ) {
        TabItem(
            selected = selectedTab == 0,
            label = "首页",
            icon = Icons.Outlined.Home,
            selectedIcon = Icons.Rounded.Home,
            onClick = { onSelect(0) }
        )
        TabItem(
            selected = selectedTab == 1,
            label = "我的",
            icon = Icons.Outlined.Person,
            selectedIcon = Icons.Rounded.Person,
            onClick = { onSelect(1) }
        )
    }
}

@Composable
private fun RowScope.TabItem(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    selectedIcon: ImageVector,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(38.dp), contentAlignment = Alignment.Center) {
            // 选中时出现的渐变胶囊底
            androidx.compose.animation.AnimatedVisibility(
                visible = selected,
                enter = fadeIn(tween(220)) + scaleIn(tween(260), initialScale = 0.5f),
                exit = fadeOut(tween(140)) + scaleOut(tween(160), targetScale = 0.6f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(BrandGreen, BrandMint)))
                )
            }
            // 图标缩放淡入切换
            AnimatedContent(
                targetState = selected,
                transitionSpec = {
                    (fadeIn(tween(200)) + scaleIn(tween(220), initialScale = 0.7f)) togetherWith
                        (fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.7f))
                },
                label = "tabIcon"
            ) { isSelected ->
                Icon(
                    imageVector = if (isSelected) selectedIcon else icon,
                    contentDescription = label,
                    tint = if (isSelected) Color(0xFF07120B) else inactiveColor,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else inactiveColor
        )
    }
}

/** 路由切换动画 */
private fun rootTransition(initial: RootRoute, target: RootRoute): ContentTransform {
    return when {
        target == RootRoute.PLAYER ->
            (fadeIn(tween(360)) + slideInVertically(tween(420)) { it / 8 }) togetherWith
                fadeOut(tween(240))

        initial == RootRoute.PLAYER ->
            fadeIn(tween(320)) togetherWith
                (fadeOut(tween(280)) + slideOutVertically(tween(380)) { it / 8 })

        else ->
            (fadeIn(tween(280)) + slideInHorizontally(tween(320)) { it / 16 }) togetherWith
                (fadeOut(tween(180)) + slideOutHorizontally(tween(220)) { -it / 24 })
    }
}
