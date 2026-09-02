# 小银子 Mac 实时歌词协议 v1

这份文档用于后续实现 macOS 菜单栏歌词应用。Android 是歌词和播放状态的唯一数据源，Mac 不需要维护一套同名歌词文件。

## 1. 连接角色

```text
Android 小银子                         macOS 菜单栏应用
      │                                      │
      │  Bonjour 查找 _xiaoyinzi-lyric._tcp  │
      │─────────────────────────────────────>│
      │            TCP 连接                  │
      │─────────────────────────────────────>│
      │ hello / pair / track / sync          │
      │─────────────────────────────────────>│
      │ pair_required / paired / ready       │
      │<─────────────────────────────────────│
```

- Mac 是 TCP 服务端，并通过 Bonjour 广播服务。
- Android 是 TCP 客户端，负责自动发现和断线重连。
- 两台设备必须在同一局域网，且网络不能开启客户端隔离。
- 协议为 UTF-8 编码的 NDJSON：每条 JSON 消息占一行，以 `\n` 结束。
- 当前协议版本为 `1`。

## 2. Bonjour 服务

Mac 发布：

| 字段 | 值 |
| --- | --- |
| Service type | `_xiaoyinzi-lyric._tcp` |
| Domain | `local.` |
| Service name | 任意可读名称，建议直接使用电脑名称 |
| Port | Mac 监听器实际分配的 TCP 端口 |

Swift 可使用 `Network.framework` 的 `NWListener`，并设置：

```swift
listener.service = NWListener.Service(name: Host.current().localizedName, type: "_xiaoyinzi-lyric._tcp")
```

Android 不会按 Service name 过滤设备，因此 Mac 不需要叫“小银子的 Mac”。Service type 必须保持一致；其中 `xiaoyinzi-lyric` 正好 15 个字符，符合 Bonjour 服务类型长度限制。Android 暂时也会搜索旧的 `_xiaoyinzi-lyrics._tcp`，方便迁移，但 Mac 端应改用上面的新类型。

## 3. 首次连接和配对

Android 建立 TCP 连接后首先发送 `hello`：

```json
{"type":"hello","protocolVersion":1,"deviceId":"稳定的 UUID","deviceName":"小银子 Android","token":null}
```

Mac 的处理规则：

1. 若 `protocolVersion` 不支持，返回 `error` 并关闭连接。
2. 若 token 有效，返回 `ready`。
3. 若没有 token 或 token 无效，在 Mac 界面生成并显示 6 位数字，返回 `pair_required`。
4. Android 用户输入数字后发送 `pair`。
5. 验证成功后，Mac 生成高熵随机 token，安全保存，并返回 `paired`。

Mac → Android：

```json
{"type":"pair_required"}
```

Android → Mac：

```json
{"type":"pair","code":"123456","deviceId":"稳定的 UUID"}
```

Mac → Android：

```json
{"type":"paired","token":"Mac 生成的随机 token"}
```

已有有效配对时：

```json
{"type":"ready"}
```

建议配对码 5 分钟失效、限制尝试次数，token 至少使用 32 个随机字节。Mac 应将 token 绑定到 `deviceId`，并提供移除已配对设备的入口。

## 4. 歌曲与完整歌词

Mac 返回 `paired` 或 `ready` 后，Android 会发送当前歌曲快照。切歌时也会重新发送：

```json
{
  "type": "track",
  "protocolVersion": 1,
  "trackId": "音频 URI 的 SHA-256，不包含原始文件路径",
  "title": "棠梨煎雪",
  "artist": "银临",
  "durationMs": 245000,
  "positionMs": 12500,
  "isPlaying": true,
  "playbackSpeed": 1.0,
  "sentAtEpochMs": 1700000000000,
  "lyricsHash": "歌词内容的 SHA-256",
  "lyrics": [
    {"timeMs": 5200, "text": "第一句歌词"},
    {"timeMs": 11850, "text": "第二句歌词"}
  ]
}
```

Android 不发送音乐文件、不发送 `content://` URI，也不要求 Mac 本地存在 `.lrcx`。没有歌词时 `lyrics` 是空数组，Mac 可以显示歌曲名或“暂无歌词”。

## 5. 播放位置同步

Android 播放期间每 750ms 发送一次 `sync`，在播放、暂停、跳转和播放状态变化时也会立即发送：

```json
{
  "type": "sync",
  "trackId": "与 track 消息相同",
  "positionMs": 13250,
  "durationMs": 245000,
  "isPlaying": true,
  "playbackSpeed": 1.0,
  "sentAtEpochMs": 1700000000750
}
```

Mac 端建议按下面方式估算当前位置，避免 750ms 一跳：

```text
若正在播放：
显示位置 = positionMs + (当前时间 - 收到消息时间) × playbackSpeed

若已暂停：
显示位置 = positionMs
```

收到乱序或旧 `trackId` 的 `sync` 时直接忽略。当前歌词行为：取 `timeMs <= 显示位置` 的最后一行。

## 6. 错误消息

Mac 可以发送：

```json
{"type":"error","message":"配对码错误"}
```

无法继续时应在发送后关闭 TCP；Android 会以 1、2、4、8、15 秒的上限间隔自动重连。未知 JSON 字段应忽略，以便以后兼容扩展。

## 7. Mac 端最小实现清单

- 使用 `NWListener` 监听 TCP 并发布 Bonjour 服务。
- 用缓冲区按换行切分消息；不要假设一次网络回调就是一条完整 JSON。
- 实现 `hello`、`pair`、`track`、`sync` 四类入站消息。
- 实现 `pair_required`、`paired`、`ready`、`error` 四类出站消息。
- 将 token 安全保存在 Keychain。
- 依据 `sentAtEpochMs`/本地接收时间平滑推进歌词，并处理暂停与跳转。
- 在菜单栏应用退出或网络切换时正常取消监听器。

## 8. Android 端对应代码

| 职责 | 文件 |
| --- | --- |
| 消息模型与常量 | `casting/CastProtocol.kt` |
| Bonjour 发现 | `casting/MacServiceDiscovery.kt` |
| TCP、配对、重连和同步 | `casting/LyricsCastManager.kt` |
| 播放器状态接入 | `playback/PlaybackService.kt` |
| 连接界面 | `ui/PlayerApp.kt` |
