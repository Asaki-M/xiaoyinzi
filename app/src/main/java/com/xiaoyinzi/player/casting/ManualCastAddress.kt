package com.xiaoyinzi.player.casting

internal fun parseManualCastAddress(value: String): CastDevice {
    val parts = value.trim().split(':')
    require(parts.size == 2) { "请输入 Mac 显示的 IP:端口，例如 192.168.1.10:49200" }
    val octets = parts[0].split('.')
    require(octets.size == 4 && octets.all { octet ->
        octet.isNotEmpty() && octet.length <= 3 && octet.all { it in '0'..'9' } &&
            octet.toInt() in 0..255
    }) { "IP 地址格式不正确，请照着 Mac 显示的地址输入" }
    val numbers = octets.map(String::toInt)
    require(numbers[0] in 1..223 && numbers[0] != 127) {
        "请输入 Mac 的局域网 IP 地址"
    }
    val port = parts[1].takeIf { it.isNotEmpty() && it.all { char -> char in '0'..'9' } }?.toIntOrNull()
    require(port != null && port in 1..65535) { "端口应为 1–65535，请使用 Mac 显示的端口" }
    val host = numbers.joinToString(".")
    return CastDevice("manual@$host:$port", "Mac（$host:$port）", host, port)
}
