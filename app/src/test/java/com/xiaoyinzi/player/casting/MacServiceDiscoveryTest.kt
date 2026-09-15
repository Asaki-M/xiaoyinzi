package com.xiaoyinzi.player.casting

import java.net.InetAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MacServiceDiscoveryTest {
    @Test
    fun `LAN IPv4 is selected instead of advertised loopback or IPv6`() {
        val lan = InetAddress.getByName("192.168.155.171")
        val selected = selectCastAddress(
            listOf(
                InetAddress.getByName("127.0.0.1"),
                InetAddress.getByName("::1"),
                InetAddress.getByName("fe80::1234"),
                lan,
            ),
        )

        assertEquals(lan, selected)
    }

    @Test
    fun `unusable broadcast addresses do not become connectable devices`() {
        assertNull(selectCastAddress(emptyList()))
        assertNull(
            selectCastAddress(
                listOf("127.0.0.1", "127.0.0.2", "::1", "0.0.0.0", "::", "224.0.0.251", "ff02::fb")
                    .map(InetAddress::getByName),
            ),
        )
    }

    @Test
    fun `IPv6 receivers remain supported when no IPv4 is available`() {
        val lan = InetAddress.getByName("fd00::1234")
        assertEquals(
            lan,
            selectCastAddress(listOf(InetAddress.getByName("fe80::1234"), lan)),
        )
    }
}
