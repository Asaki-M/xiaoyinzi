package com.xiaoyinzi.player.library

import com.xiaoyinzi.player.data.TrackEntity
import com.xiaoyinzi.player.data.TrackGroupCrossRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryGroupsTest {
    @Test
    fun allTracksCanBeCopiedWithoutDuplicatingUris() {
        val first = track("故城")
        val second = track("不老梦")

        assertEquals(
            listOf(first.uri, second.uri),
            groupTrackUrisForCopy(null, listOf(first, second, first), emptyList()),
        )
    }

    @Test
    fun customCopyIncludesOnlyExistingTracksFromTheSourceGroup() {
        val first = track("故城")
        val second = track("不老梦")
        val memberships = listOf(
            TrackGroupCrossRef(first.uri, 1),
            TrackGroupCrossRef(second.uri, 2),
            TrackGroupCrossRef("file:///missing.mp3", 1),
        )

        assertEquals(
            listOf(first.uri),
            groupTrackUrisForCopy(customGroupId(1), listOf(first, second), memberships),
        )
        assertEquals(3, memberships.size)
    }

    @Test
    fun presetCopyIncludesImportedMatchesAndSkipsMissingSongs() {
        val imported = track("01. 故城 (feat. 灰原穷)")
        val unrelated = track("不老梦")

        assertEquals(
            listOf(imported.uri),
            groupTrackUrisForCopy(albumGroupId("fu-cao-wei-ying"), listOf(imported, unrelated), emptyList()),
        )
    }

    @Test
    fun emptyGroupsCanBeCopied() {
        assertTrue(groupTrackUrisForCopy(customGroupId(1), listOf(track("故城")), emptyList()).isEmpty())
        assertTrue(groupTrackUrisForCopy(albumGroupId("fu-cao-wei-ying"), emptyList(), emptyList()).isEmpty())
        assertTrue(groupTrackUrisForCopy(null, emptyList(), emptyList()).isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownPresetCannotBeCopiedAsTheEntireLibrary() {
        groupTrackUrisForCopy("preset:album:missing", listOf(track("故城")), emptyList())
    }

    @Test
    fun everyPresetAndCustomGroupCanBeTheDefault() {
        SilverlinCatalog.albums.forEach { album ->
            assertTrue(isLibraryGroupAvailable(albumGroupId(album.id), emptySet(), emptySet()))
        }
        assertTrue(isLibraryGroupAvailable(customGroupId(42), setOf(42L), emptySet()))
        assertTrue(isLibraryGroupAvailable(null, emptySet(), emptySet()))
    }

    @Test
    fun deletedCustomAndHiddenPresetDefaultsAreUnavailable() {
        val presetId = albumGroupId("fu-cao-wei-ying")

        assertFalse(isLibraryGroupAvailable(customGroupId(42), emptySet(), emptySet()))
        assertFalse(isLibraryGroupAvailable(presetId, emptySet(), setOf(presetId)))
        assertTrue(isLibraryGroupAvailable(presetId, emptySet(), emptySet()))
    }

    @Test
    fun invalidStoredDefaultsAreUnavailable() {
        listOf("custom:bad", "custom:", "preset:album:missing", "unknown").forEach { id ->
            assertFalse(isLibraryGroupAvailable(id, setOf(1L), emptySet()))
        }
    }

    private fun track(title: String) = TrackEntity(
        uri = "file:///$title.mp3",
        rootUri = "file:///music",
        title = title,
        artist = "银临",
        album = "",
        durationMs = 0,
        mimeType = "audio/mpeg",
        lyricUri = "file:///$title.lrcx",
    )
}
