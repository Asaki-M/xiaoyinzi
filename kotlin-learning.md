# “小银子”项目导读

这份文档面向有编程经验、但不熟悉 Kotlin/Android 的开发者。目标不是系统学习 Kotlin，而是快速理解这个项目如何扫描音乐、保存分组、播放音频和渲染界面。

## 1. 技术栈

| 能力 | 技术 |
| --- | --- |
| 语言 | Kotlin 2.1 |
| UI | Jetpack Compose + Material 3 |
| 播放器 | AndroidX Media3 / ExoPlayer |
| 后台播放 | `MediaSessionService` |
| 本地数据库 | Room + SQLite |
| 异步 | Kotlin Coroutines |
| 响应式状态 | `Flow` / `StateFlow` |
| 文件访问 | Storage Access Framework + `DocumentFile` |
| 局域网歌词 | Android NSD / Bonjour + TCP + Kotlin Serialization |
| 构建 | Gradle Kotlin DSL + KSP |

应用没有云端服务。联网权限只用于用户主动开启的同一局域网 Mac 歌词同步。

## 2. 项目结构

```text
xiaoyinzi/
├── build.gradle.kts                   # 根项目插件版本
├── settings.gradle.kts                # 模块与仓库声明
├── app/build.gradle.kts               # Android 配置和依赖
└── app/src/main/
    ├── AndroidManifest.xml            # Activity、Service、权限
    ├── java/com/xiaoyinzi/player/
    │   ├── MainActivity.kt            # Android 与 Compose 的入口
    │   ├── MainViewModel.kt           # UI 状态和业务协调
    │   ├── XiaoYinZiApplication.kt    # 数据库和 Repository 实例
    │   ├── casting/                   # Mac 发现、配对和歌词同步
    │   ├── data/                      # Room 数据层
    │   ├── library/                   # 文件扫描与资料库业务
    │   ├── lyrics/                    # .lrcx 解析
    │   ├── playback/                  # Media3 播放服务与控制器
    │   └── ui/                        # Compose 界面和主题
    └── res/                           # 图片、图标、主题、字符串
```

项目是单 `app` 模块，没有引入 Hilt、Navigation Compose 或多模块架构，调用链比较直接。

## 3. 应用启动链路

启动顺序：

```text
Android 系统
  ↓
XiaoYinZiApplication
  ↓
MainActivity
  ↓
MainViewModel
  ├── LibraryRepository / Room
  ├── LibraryScanner
  ├── LrcxParser
  └── PlayerConnection
        ↓
     PlaybackService
        ↓
     ExoPlayer + MediaSession
```

### `XiaoYinZiApplication`

文件：[XiaoYinZiApplication.kt](app/src/main/java/com/xiaoyinzi/player/XiaoYinZiApplication.kt)

这是一个简单的依赖容器：

```kotlin
class XiaoYinZiApplication : Application() {
    val database by lazy { ... }
    val libraryRepository by lazy { ... }
    val libraryScanner by lazy { ... }
    val lyricParser by lazy { ... }
}
```

`by lazy` 表示第一次访问时才创建实例。它在这里代替了依赖注入框架。

### `MainActivity`

文件：[MainActivity.kt](app/src/main/java/com/xiaoyinzi/player/MainActivity.kt)

职责只有三件事：

1. 获取 `MainViewModel`。
2. 注册系统目录选择器和通知权限请求。
3. 调用 `setContent` 启动 Compose UI。

目录选择使用：

```kotlin
ActivityResultContracts.OpenDocumentTree()
```

因此应用不需要申请“管理所有文件”权限。

## 4. 数据模型与 Room

文件：

- [LibraryEntities.kt](app/src/main/java/com/xiaoyinzi/player/data/LibraryEntities.kt)
- [LibraryDao.kt](app/src/main/java/com/xiaoyinzi/player/data/LibraryDao.kt)
- [AppDatabase.kt](app/src/main/java/com/xiaoyinzi/player/data/AppDatabase.kt)

### 表结构

```text
tracks
  uri (PK)
  rootUri
  title
  artist
  album
  durationMs
  mimeType
  lyricUri

music_groups
  id (PK)
  name
  createdAt

track_group_cross_ref
  trackUri (FK)
  groupId (FK)
```

歌曲与分组是多对多关系：

```text
Track ──< TrackGroupCrossRef >── Group
```

一个分组可以包含多首歌，一首歌也可以加入多个分组。

### Entity

```kotlin
@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val uri: String,
    val title: String,
    ...
)
```

`data class` 主要用于数据载体，Kotlin 自动生成 `equals`、`hashCode`、`copy` 和 `toString`。

### DAO

DAO 用注解声明 SQL：

```kotlin
@Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE")
fun observeTracks(): Flow<List<TrackEntity>>
```

返回 `Flow` 后，数据库内容变化会自动推送到 ViewModel，不需要手动刷新 UI。

### Repository

文件：[LibraryRepository.kt](app/src/main/java/com/xiaoyinzi/player/library/LibraryRepository.kt)

Repository 封装 DAO，并负责扫描结果的事务更新：

```kotlin
database.withTransaction {
    dao.deleteTracksOutsideRoot(rootUri)
    dao.upsertTracks(tracks)
    dao.deleteTracks(removedUris)
}
```

当前设计只保留一个音乐根目录。用户切换目录时，旧目录的歌曲索引会删除。

## 5. 本地音乐扫描

文件：[LibraryScanner.kt](app/src/main/java/com/xiaoyinzi/player/library/LibraryScanner.kt)

核心流程：

```text
用户选择目录 URI
  ↓
DocumentFile.fromTreeUri
  ↓
递归 collectFiles
  ↓
区分音频与 .lrcx
  ↓
MediaMetadataRetriever 读取元数据
  ↓
生成 List<TrackEntity>
  ↓
Repository 写入 Room
```

支持的扩展名：

```kotlin
setOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "opus")
```

音频判断同时检查 MIME 类型和扩展名：

```kotlin
return type?.startsWith("audio/") == true || extension in audioExtensions
```

### 歌词匹配

音乐与歌词使用“父目录 URI + 不带扩展名的文件名”匹配：

```text
/Music/银临/棠梨煎雪.flac
/Music/银临/棠梨煎雪.lrcx
```

这可以避免不同子目录中同名歌曲串歌词。

### SAF 权限

`MainViewModel.selectFolder` 会保存系统授予的目录权限：

```kotlin
contentResolver.takePersistableUriPermission(...)
```

目录 URI 还会写入 `SharedPreferences`。下次启动时，ViewModel 会自动重新扫描。

## 6. 播放架构

文件：

- [PlaybackService.kt](app/src/main/java/com/xiaoyinzi/player/playback/PlaybackService.kt)
- [PlayerConnection.kt](app/src/main/java/com/xiaoyinzi/player/playback/PlayerConnection.kt)

播放分成两个部分：

```text
UI 进程中的 PlayerConnection
          ↓ MediaController
后台 PlaybackService
          ↓
MediaSession
          ↓
ExoPlayer
```

### `PlaybackService`

服务创建 `ExoPlayer`：

```kotlin
val player = ExoPlayer.Builder(this)
    .setAudioAttributes(audioAttributes, true)
    .setHandleAudioBecomingNoisy(true)
    .build()
```

- `setAudioAttributes(..., true)`：让 Media3 管理音频焦点。
- `setHandleAudioBecomingNoisy(true)`：耳机断开时自动处理外放风险。
- `MediaSession`：向通知栏、锁屏、蓝牙设备暴露统一媒体控制。

`MediaSessionService` 会负责后台播放生命周期和媒体通知。

### `PlayerConnection`

UI 不直接持有 ExoPlayer，而是异步连接服务：

```kotlin
MediaController.Builder(applicationContext, token)
    .buildAsync()
```

连接成功后，通过 `Player.Listener` 监听播放变化，并转换为：

```kotlin
data class PlayerUiState(
    val isPlaying: Boolean,
    val currentTrackUri: String?,
    val positionMs: Long,
    val durationMs: Long,
    val shuffleEnabled: Boolean,
    val repeatMode: Int,
)
```

播放进度不会持续触发 Player 事件，因此另外启动协程，每 250ms 读取一次当前位置。

### 播放队列

点击歌曲时，当前界面可见的歌曲列表会成为播放队列：

```kotlin
player.setMediaItems(mediaItems, selectedIndex, 0)
player.prepare()
player.play()
```

如果当前查看某个分组，播放队列就是该分组；如果查看全部歌曲，队列就是整个资料库。

随机与循环直接映射到 Media3：

```kotlin
player.shuffleModeEnabled = true
player.repeatMode = Player.REPEAT_MODE_ALL
```

## 7. ViewModel 与状态流

文件：[MainViewModel.kt](app/src/main/java/com/xiaoyinzi/player/MainViewModel.kt)

ViewModel 是整个项目的协调层，但不直接实现扫描、SQL 或播放细节。

```text
Compose UI
  ↓ 用户操作
MainViewModel
  ├── 调 LibraryScanner
  ├── 调 LibraryRepository
  └── 调 PlayerConnection
  ↓ StateFlow
Compose UI 自动重组
```

资料库状态：

```kotlin
data class LibraryUiState(
    val tracks: List<TrackEntity>,
    val groups: List<GroupSummary>,
    val selectedGroupId: Long?,
    val scanning: Boolean,
    val message: String?,
)
```

多个 Flow 使用 `combine` 合并：

```kotlin
combine(
    visibleTracks,
    repository.groups,
    selectedGroupId,
    scanning,
    message,
) { ... }
```

`stateIn` 把冷 Flow 转换成可随时读取当前值的 `StateFlow`。

### 为什么使用 `viewModelScope`

```kotlin
viewModelScope.launch {
    val tracks = scanner.scan(uri)
}
```

`viewModelScope` 会在 ViewModel 销毁时自动取消协程，避免 Activity 销毁后扫描仍然持有界面引用。

## 8. `.lrcx` 歌词

文件：[LrcxParser.kt](app/src/main/java/com/xiaoyinzi/player/lyrics/LrcxParser.kt)

支持：

```text
[00:05.20]行歌词
[00:18.00][00:42.00]重复歌词
<00:52.00>逐<00:52.20>字
```

解析结果：

```kotlin
data class LyricLine(
    val timeMs: Long,
    val text: String,
)
```

解析器用正则抽取时间标签，统一换算为毫秒，最后按时间排序。

目前逐字标签只会去掉标签，并用第一个逐字时间作为整行时间；还没有实现逐字高亮。

ViewModel 会组合“当前歌曲”和“资料库歌曲信息”，找到对应 `lyricUri` 后加载歌词。

## 9. Compose UI

文件：

- [PlayerApp.kt](app/src/main/java/com/xiaoyinzi/player/ui/PlayerApp.kt)
- [AppTheme.kt](app/src/main/java/com/xiaoyinzi/player/ui/AppTheme.kt)
- [Typography.kt](app/src/main/java/com/xiaoyinzi/player/ui/Typography.kt)

入口：

```kotlin
@Composable
fun PlayerApp(viewModel: MainViewModel, onChooseFolder: () -> Unit)
```

状态通过生命周期安全的方式订阅：

```kotlin
val library by viewModel.uiState.collectAsStateWithLifecycle()
val player by viewModel.playerState.collectAsStateWithLifecycle()
val lyrics by viewModel.lyrics.collectAsStateWithLifecycle()
```

StateFlow 更新后，读取这些状态的 Composable 会自动重新执行。

主要组件：

```text
PlayerApp
├── LibraryScreen
│   ├── LibraryHeader
│   ├── GroupSelector
│   ├── TrackList
│   └── EmptyLibrary
├── MiniPlayer
├── CreateGroupDialog
└── NowPlayingScreen
    ├── VinylArtwork
    └── 歌词 LazyColumn
```

### Compose 的状态

只在当前组件内部使用的临时状态用 `remember`：

```kotlin
var menuOpen by remember { mutableStateOf(false) }
```

业务状态放在 ViewModel，例如歌曲列表、当前分组和播放状态。Activity 重建时，ViewModel 状态更容易保留。

### 列表

歌曲使用 `LazyColumn`，只组合屏幕附近的项目：

```kotlin
items(tracks, key = TrackEntity::uri) { track ->
    TrackRow(...)
}
```

`key` 使用稳定 URI，减少列表变化时不必要的 UI 重建。

### 歌词同步

```kotlin
val currentLine = lyrics.indexOfLast {
    it.timeMs <= state.positionMs
}
```

当前行改变后：

```kotlin
lyricListState.animateScrollToItem(currentLine, -120)
```

点击歌词则调用 `onSeek(line.timeMs)` 跳转播放位置。

## 10. 一次完整播放的数据流

```text
1. 用户选择目录
2. MainActivity 把 Uri 交给 MainViewModel
3. LibraryScanner 扫描音乐与歌词
4. LibraryRepository 写入 Room
5. Room Flow 推送新的歌曲列表
6. MainViewModel 生成 LibraryUiState
7. Compose 重组 TrackList
8. 用户点击歌曲
9. MainViewModel.play(track)
10. PlayerConnection 将列表转换为 MediaItem 队列
11. MediaController 把命令发给 PlaybackService
12. ExoPlayer 播放 content:// URI
13. MediaSession 更新通知栏、锁屏和蓝牙状态
14. PlayerConnection 更新 PlayerUiState
15. Compose 更新迷你播放器、进度和歌词
16. 若已连接 Mac，PlaybackService 同步当前歌曲、歌词和播放位置
```

## 11. Mac 实时歌词是怎么工作的

文件：

- [CastProtocol.kt](app/src/main/java/com/xiaoyinzi/player/casting/CastProtocol.kt)
- [MacServiceDiscovery.kt](app/src/main/java/com/xiaoyinzi/player/casting/MacServiceDiscovery.kt)
- [LyricsCastManager.kt](app/src/main/java/com/xiaoyinzi/player/casting/LyricsCastManager.kt)
- [mac-lyrics-protocol.md](mac-lyrics-protocol.md)

这部分让 Android 成为歌词数据源，未来的 Mac 菜单栏应用只负责显示：

```text
PlaybackService / ExoPlayer
          ↓ Player.Listener
LyricsCastManager ← LrcxParser
          ↓ UTF-8 NDJSON over TCP
同一局域网中的 Mac 菜单栏应用
```

`MacServiceDiscovery` 使用 Android NSD 搜索 Bonjour 服务 `_xiaoyinzi-lyrics._tcp.`。选择设备后，`LyricsCastManager` 建立 TCP 连接，首次使用 6 位码配对；以后用保存的 token 自动连接。

切歌时会发送完整 `TrackMessage`，其中带有解析后的歌词行；播放期间每 750ms 发送轻量 `SyncMessage`。播放、暂停和跳转时还会立即同步。这里的协程运行在应用级 `SupervisorJob` 中，因此 Compose 页面关闭不会让后台播放的歌词连接一起消失。

`kotlinx.serialization` 根据 `@Serializable data class` 生成 JSON 编解码代码：

```kotlin
@Serializable
data class SyncMessage(
    val type: String = "sync",
    val positionMs: Long,
    ...
)

val line = json.encodeToString(message)
```

TCP 是字节流，不保留“消息边界”，所以协议规定每条 JSON 后加换行。Mac 端要先按换行拆包，再解析 JSON。

## 12. 常见修改应该去哪里

| 需求 | 修改位置 |
| --- | --- |
| 增加音频格式 | `LibraryScanner.audioExtensions` |
| 改歌词格式 | `LrcxParser.kt` |
| 改 Mac 歌词协议 | `CastProtocol.kt`、`LyricsCastManager.kt` |
| 改扫描规则 | `LibraryScanner.kt` |
| 增加歌曲字段 | Entity → DAO → Scanner → UI |
| 改数据库查询 | `LibraryDao.kt` |
| 改分组行为 | `LibraryRepository.kt`、`MainViewModel.kt` |
| 改播放逻辑 | `PlayerConnection.kt` |
| 改后台服务 | `PlaybackService.kt` |
| 改主界面 | `PlayerApp.kt` |
| 改颜色字体 | `AppTheme.kt`、`Typography.kt` |
| 改应用名 | `res/values/strings.xml` |
| 改权限或组件 | `AndroidManifest.xml` |

增加数据库字段时，需要同时升级 Room `version` 并提供 Migration；开发阶段也可以清除应用数据重新创建数据库。

## 13. 项目里常见的 Kotlin 写法

不单独学习语法时，只需要先认识这些：

```kotlin
// 安全调用：左边是 null 时不继续执行
uri?.let { scan(it) }

// Elvis：左边为 null 时使用右边
artist ?: "未知音乐人"

// Lambda
tracks.filter { it.artist == "银临" }

// 函数引用
onClick = viewModel.player::togglePlayPause

// 协程
viewModelScope.launch { repository.createGroup(name) }

// Flow 转换
selectedGroupId.flatMapLatest { groupId -> ... }

// 属性委托
val database by lazy { ... }
val state by flow.collectAsStateWithLifecycle()

// 作用域函数
controller?.let { player -> ... }
runCatching { scanner.scan(uri) }.onFailure { ... }
```

## 14. 推荐阅读顺序

不要从 UI 大文件开始逐行看，建议按这条链路：

1. `LibraryEntities.kt`：先认识项目数据。
2. `LibraryScanner.kt`：理解音乐怎么进入系统。
3. `LibraryRepository.kt` 和 `LibraryDao.kt`：理解数据怎么保存。
4. `PlaybackService.kt`：理解真正的播放器在哪里。
5. `PlayerConnection.kt`：理解 UI 如何控制播放器。
6. `MainViewModel.kt`：理解各模块怎么组合。
7. `PlayerApp.kt`：最后看界面如何消费状态。
8. `LrcxParser.kt`：独立功能，随时都可以看。
9. `casting/`：最后看局域网发现、Socket 与序列化。

## 15. 构建与调试

编译、测试和 Lint：

```bash
./gradlew testDebugUnitTest assembleDebug lintDebug
```

APK：

```text
app/build/outputs/apk/debug/app-debug.apk
```

查看日志：

```bash
adb logcat | rg 'xiaoyinzi|AndroidRuntime|Media3'
```

安装新包：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 16. 当前已知边界

- 只支持一个音乐根目录。
- 未读取音频内嵌封面，当前银临图片是粉丝主题静态素材。
- `.lrcx` 暂时是逐行同步，不是逐字高亮。
- 播放队列没有单独的可视化管理页面。
- 没有保存上次播放位置和进程被杀后的队列恢复。
- MediaController 异步连接完成前立即点击歌曲，命令可能不会执行。
- Debug APK 使用系统 debug key，正式发布需要配置 release 签名。
- 蓝牙真实兼容性应在 Android 真机上验证。
- Mac 实时歌词需要另行实现并启动 macOS 接收端后才能端到端联调。
