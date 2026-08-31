# 小银子

一个完全离线的 Android 本地音乐播放器，使用 Kotlin、Jetpack Compose 与 AndroidX Media3。

## 已实现

- 通过系统目录选择器指定本机音乐目录，并持久保存读取权限
- 递归扫描 MP3、M4A、AAC、FLAC、WAV、OGG、OPUS
- 读取歌曲标题、音乐人、专辑与时长
- Room 本地资料库，可创建/删除分组、把歌曲加入或移出分组
- Media3 `MediaSessionService` 后台播放
- 系统媒体通知、锁屏控制和蓝牙耳机播放/暂停/切歌
- 顺序队列、随机播放、列表循环、单曲循环
- 同目录、同文件名 `.lrcx` 歌词匹配
- 歌词时间轴高亮，点按歌词跳转播放位置

所有目录、歌曲索引和分组信息只保存在设备本地，没有服务端和联网权限。

## 银临粉丝视觉

- 宣纸白、雾青与萤火金配色，衬线标题配合克制的现代排版
- 资料库顶部使用银临公开艺人图，空状态使用《腐草为萤》《蚍蜉渡海》封面
- 播放页使用原创月下水墨背景，唱片以水纹形式缓慢旋转
- 萤火呼吸、歌词滚动、播放页展开三组轻量动效

艺人图与专辑封面仅用于个人粉丝向应用。若公开发布或商业分发，请替换为取得正式授权的素材。原创水墨素材位于
`app/src/main/res/drawable-nodpi/ink_water_scene.png`。

## `.lrcx` 约定

首版将 `.lrcx` 当作 UTF-8 的时间标签歌词文本，支持：

```text
[ar:音乐人]
[ti:歌曲名]
[00:05.20]第一句歌词
[00:11.850]第二句歌词
[00:18.00][00:42.00]重复出现的歌词
<00:52.00>逐<00:52.20>字<00:52.40>标签
```

歌词与音乐需要同名，例如：

```text
Music/
├── 晚风.flac
└── 晚风.lrcx
```

逐字标签首版会清理标签并按首个时间点高亮整行。如果实际 `.lrcx` 是 JSON、加密文本或其他专用格式，只需扩展
`app/src/main/java/com/xiaoyinzi/player/lyrics/LrcxParser.kt`，其他模块无需修改。

## 构建与运行

1. 使用 Android Studio 打开项目。
2. 安装 JDK 17、Android SDK 35。
3. 等待 Gradle 同步后运行 `app`，或执行：

```bash
./gradlew testDebugUnitTest assembleDebug
```

首次打开会请求通知权限；点击右上角目录图标选择音乐目录。应用通过 Storage Access Framework 获取目录权限，不需要“所有文件访问”权限。

## 工程结构

```text
app/src/main/java/com/xiaoyinzi/player/
├── data/       # Room 实体、DAO 与数据库
├── library/    # 目录扫描和资料库业务
├── lyrics/     # .lrcx 解析与时间轴模型
├── playback/   # Media3 后台服务和控制器
└── ui/         # Compose 界面与主题
```
