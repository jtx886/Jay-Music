# Jay Music

一款原生 Android 音乐播放器，采用 Kotlin + Jetpack Compose + Material3 + Media3(ExoPlayer) 构建，
视觉风格参考 Spotify 与网易云音乐，支持深色 / 浅色双主题、毛玻璃与渐变界面、共享元素转场与逐字歌词。

**应用完全本地运行，无需自建后端、无需邮箱验证码。** 账号、收藏与听歌历史均保存在本机。

## 功能特性

- 本地账号：用户名 + 密码注册 / 登录，密码 PBKDF2 加盐哈希后加密保存
- 本地头像：从系统相册选择，压缩后保存在应用私有目录
- 音乐发现：汽水音乐 + 网易云双音源搜索、自动降级与音质切换
- 播放能力：后台播放、通知栏与锁屏控制、音频焦点处理、播放模式切换
- 歌词体验：LRC 逐行歌词与 YRC 逐字高亮、翻译歌词、点击歌词跳转播放进度
- 个人曲库：我的收藏、听歌历史、一键清空（本地持久化）
- 精致交互：SharedElement 迷你播放器 → 全屏播放器转场、列表淡入位移动画、骨架屏、空状态、封面主色调动态背景
- 安全存储：账号密码哈希与会话使用 EncryptedSharedPreferences 加密保存

## 技术栈

| 模块 | 方案 |
| --- | --- |
| UI | Jetpack Compose、Material3、Animation、SharedTransition |
| 播放 | AndroidX Media3 / ExoPlayer、MediaSessionService |
| 网络 | OkHttp、Gson（仅第三方音乐接口） |
| 图片 | Coil、Palette 主色提取 |
| 存储 | EncryptedSharedPreferences、SharedPreferences |
| 账号 | 本机 PBKDF2 密码哈希，无后端 |

- `minSdk 24` / `targetSdk 34` / `compileSdk 34`
- Kotlin 2.x + JDK 17

## 项目结构

```
app/src/main/java/com/jay/music/
├── core/          全局服务定位器、会话管理
├── data/
│   ├── local/     本机账号、加密存储、普通配置
│   ├── remote/    第三方音乐接口
│   ├── repository/ 账号、曲库、音乐仓库
│   └── model/     数据模型
├── player/        播放控制器与 MediaSession
├── ui/
│   ├── components/ 通用 UI 组件（毛玻璃、渐变、骨架屏、迷你播放器）
│   ├── navigation/ 根导航与页面转场
│   ├── screens/    登录、注册、首页、搜索、我的、收藏、历史、播放器
│   └── theme/      双主题与配色
└── util/          校验、时间格式化、歌词解析
```

## 构建

1. 使用 Android Studio 打开项目，等待 Gradle 同步完成。
2. 构建：

```bash
./gradlew assembleDebug
# 或发布包（需自备签名，见下）
./gradlew assembleRelease
```

生成的 APK 位于 `app/build/outputs/apk/`。

### Release 签名

在项目根目录创建 `keystore.properties`（已被 `.gitignore` 忽略）：

```properties
storeFile=keystore/jay-music-release.jks
storePassword=你的密码
keyAlias=jaymusic
keyPassword=你的密码
```

未提供该文件时仍可构建未签名的 Release 包。

## 账号说明

- 首次使用请点击「立即注册」创建本机账号，随后登录。
- 账号仅保存在当前设备，卸载应用或清除应用数据会一并删除账号、收藏与历史。
- 出于无后端的限制，不提供邮箱验证与密码找回功能。

## 下载

前往 [Releases](https://github.com/jtx886/Jay-Music/releases) 下载最新 APK。

> 安装前请允许「安装未知来源应用」。App 内音乐数据来自第三方公开接口，仅供学习交流使用。
