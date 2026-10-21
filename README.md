# Jay Music

一款原生 Android 音乐播放器，采用 Kotlin + Jetpack Compose + Material3 + Media3(ExoPlayer) 构建，
视觉风格参考 Spotify 与网易云音乐，支持深色 / 浅色双主题、毛玻璃与渐变界面、共享元素转场与逐字歌词。

## 功能特性

- 账号体系：邮箱验证码注册、登录、忘记密码重置（后端 SMTP 发信）
- 音乐发现：搜索网易云 / 酷我双音源，自动降级与音源切换、音质切换
- 播放能力：后台播放、通知栏与锁屏控制、音频焦点处理、播放模式切换
- 歌词体验：LRC 逐行歌词与 YRC 逐字高亮、翻译歌词、点击歌词跳转播放进度
- 个人曲库：我的收藏、听歌历史、一键清空
- 精致交互：SharedElement 迷你播放器 → 全屏播放器转场、列表淡入位移动画、骨架屏、空状态、封面主色调动态背景
- 安全存储：Token 与用户信息使用 EncryptedSharedPreferences 加密保存

## 技术栈

| 模块 | 方案 |
| --- | --- |
| UI | Jetpack Compose、Material3、Animation、SharedTransition |
| 播放 | AndroidX Media3 / ExoPlayer、MediaSessionService |
| 网络 | Retrofit2、OkHttp、Gson |
| 图片 | Coil、Palette 主色提取 |
| 存储 | EncryptedSharedPreferences、SharedPreferences |
| 后端 | PHP 8 + MySQL / MariaDB + 原生 SMTP |

- `minSdk 24` / `targetSdk 34` / `compileSdk 34`
- Kotlin 2.x + JDK 17

## 项目结构

```
app/src/main/java/com/jay/music/
├── core/          全局服务定位器、会话管理
├── data/          本地存储、网络层、仓库层、数据模型
├── player/        播放控制器与 MediaSession
├── ui/
│   ├── components/ 通用 UI 组件（毛玻璃、渐变、骨架屏、迷你播放器）
│   ├── navigation/ 根导航与页面转场
│   ├── screens/    登录、注册、首页、搜索、我的、收藏、历史、播放器
│   └── theme/      双主题与配色
└── util/          校验、时间格式化、歌词解析

server/
├── api/           注册 / 登录 / 验证码 / 重置密码 / 收藏 / 历史接口
├── sql/schema.sql 数据库建表脚本
├── config.php     数据库与 SMTP 配置
└── mailer.php     原生 SMTP 邮件发送
```

## 构建 Android App

1. 使用 Android Studio 打开项目，等待 Gradle 同步完成。
2. 修改后端地址（模拟器访问宿主机为 `10.0.2.2`，真机改为电脑局域网 IP）：
   [app/build.gradle.kts](app/build.gradle.kts) 中的 `API_BASE_URL`。
3. 构建：

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

## 部署 PHP 后端

1. 导入数据库：

```bash
mysql -u root -p < server/sql/schema.sql
```

2. 配置数据库与 SMTP。推荐使用环境变量（也可直接编辑 `server/config.php`）：

```bash
export JAY_DB_HOST=127.0.0.1
export JAY_DB_NAME=jay_music
export JAY_DB_USER=root
export JAY_DB_PASS=你的数据库密码
export JAY_SMTP_HOST=smtp.163.com
export JAY_SMTP_PORT=465
export JAY_SMTP_USER=你的邮箱
export JAY_SMTP_PASS=邮箱 SMTP 授权码
export JAY_SMTP_FROM=你的邮箱
```

3. 启动（开发环境）：

```bash
php -S 0.0.0.0:8080 -t server
```

生产环境请将 Web 根目录指向 `server/`，并启用 PHP 的 `pdo_mysql` 与 `openssl` 扩展。
Apache 下已附带 `.htaccess` 保证 `Authorization` 请求头可透传。

### 接口一览

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/send_code.php` | 发送邮箱验证码 |
| POST | `/api/register.php` | 注册 |
| POST | `/api/login.php` | 登录 |
| POST | `/api/reset_password.php` | 重置密码 |
| GET | `/api/user.php` | 当前用户信息 |
| GET/POST | `/api/favorites.php` | 收藏查询 / 增删 |
| GET/POST | `/api/history.php` | 历史查询 / 上报 / 清空 |

除验证码、注册、登录、重置密码外，均需请求头 `Authorization: Bearer {token}`。

## 下载

前往 [Releases](https://github.com/jtx886/Jay-Music/releases) 下载最新 APK。

> 安装前请允许「安装未知来源应用」。App 内音乐数据来自第三方公开接口，仅供学习交流使用。
