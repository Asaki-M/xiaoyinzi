package com.xiaoyinzi.player.casting

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CastProtocolTest {
    private val json = CastJson

    @Test
    fun `track message is a single NDJSON record with lyrics`() {
        val message = TrackMessage(
            trackId = "track-hash",
            title = "棠梨煎雪",
            artist = "银临",
            durationMs = 245_000,
            positionMs = 12_500,
            isPlaying = true,
            playbackSpeed = 1f,
            sentAtEpochMs = 1_700_000_000_000,
            lyricsHash = "lyrics-hash",
            lyrics = listOf(CastLyricLine(5_200, "第一句\n仍属于同一行")),
        )

        val encoded = json.encodeToString(message)
        val payload = json.parseToJsonElement(encoded).jsonObject

        assertEquals("track", payload["type"]?.jsonPrimitive?.content)
        assertEquals(CAST_PROTOCOL_VERSION, payload["protocolVersion"]?.jsonPrimitive?.content?.toInt())
        assertEquals("棠梨煎雪", payload["title"]?.jsonPrimitive?.content)
        assertEquals(1, payload["lyrics"]?.jsonArray?.size)
        assertEquals(5_200L, payload["lyrics"]?.jsonArray?.first()?.jsonObject
            ?.get("timeMs")?.jsonPrimitive?.content?.toLong())
        assertFalse("NDJSON payload must not contain a literal line break", encoded.contains('\n'))
    }

    @Test
    fun `service type and hello protocol are stable`() {
        val payload = json.parseToJsonElement(
            json.encodeToString(HelloMessage(deviceId = "android-device")),
        ).jsonObject

        assertEquals("_xiaoyinzi-lyric._tcp", CAST_SERVICE_TYPE)
        assertEquals("_xiaoyinzi-lyrics._tcp", LEGACY_CAST_SERVICE_TYPE)
        CAST_SERVICE_TYPES.forEach { type ->
            assertFalse("Android NSD service types must not end with a dot", type.endsWith('.'))
        }
        assertEquals(15, CAST_SERVICE_TYPE.substringAfter('_').substringBefore("._tcp").length)
        assertEquals("hello", payload["type"]?.jsonPrimitive?.content)
        assertEquals(CAST_PROTOCOL_VERSION, payload["protocolVersion"]?.jsonPrimitive?.content?.toInt())
    }
}
