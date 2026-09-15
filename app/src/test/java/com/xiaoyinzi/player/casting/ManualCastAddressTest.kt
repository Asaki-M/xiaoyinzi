package com.xiaoyinzi.player.casting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ManualCastAddressTest {
    @Test
    fun `desktop address is normalized without DNS lookup`() {
        val device = parseManualCastAddress(" 192.168.001.010:49200 ")
        assertEquals("192.168.1.10", device.host)
        assertEquals(49200, device.port)
        assertEquals("Mac（192.168.1.10:49200）", device.name)
    }

    @Test
    fun `actual desktop port is preserved including fallback ports`() {
        assertEquals(50147, parseManualCastAddress("192.168.1.10:50147").port)
        assertEquals(1, parseManualCastAddress("10.0.0.2:1").port)
        assertEquals(65535, parseManualCastAddress("10.0.0.2:65535").port)
    }

    @Test
    fun `malformed or unreachable addresses are rejected before connecting`() {
        listOf(
            "", "192.168.1.10", "mac.local:49200", "http://192.168.1.10:49200",
            "192.168.1.256:49200", "192.168.1:49200", "127.0.0.1:49200",
            "0.0.0.0:49200", "224.0.0.251:49200", "255.255.255.255:49200",
            "192.168.1.10:0", "192.168.1.10:65536", "192.168.1.10:-1",
            "192.168.1.10:abc", "192.168.1.10:99999999999999", "192.168.1.10:49200/path",
            "[::1]:49200", "192.168.1.10:49200:1",
        ).forEach { address ->
            assertThrows(address, IllegalArgumentException::class.java) { parseManualCastAddress(address) }
        }
    }
}
