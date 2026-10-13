package com.xiaoyinzi.player.library

import com.xiaoyinzi.player.data.TrackEntity
import com.xiaoyinzi.player.playback.deferredQueueStartIndex
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryGroupTracksTest {
    @Test
    fun sharedCurrentTrackDoesNotCauseTheNewGroupToReuseTheOldQueue() = runBlocking {
        withTimeout(5_000) {
            val shared = track("shared")
            val oldNext = track("old-next")
            val newNext = track("new-next")
            val selected = MutableStateFlow<String?>(customGroupId(1))
            val queryStarted = CompletableDeferred<Unit>()
            val queryResult = CompletableDeferred<List<TrackEntity>>()
            val results = Channel<LibraryGroupTracks>(Channel.UNLIMITED)
            val otherLibraryState = MutableStateFlow(0)
            val groupTracks = observeGroupTracks(selected) { id ->
                if (id == customGroupId(1)) {
                    flowOf(listOf(shared, oldNext))
                } else {
                    flow {
                        queryStarted.complete(Unit)
                        emit(queryResult.await())
                    }
                }
            }
            val collector = launch {
                // Other library state can emit while the new group's database query is still loading.
                combine(groupTracks, otherLibraryState) { snapshot, _ -> snapshot }.collect { results.send(it) }
            }
            try {
                assertEquals(customGroupId(1), results.receive().groupId)
                selected.value = customGroupId(2)
                queryStarted.await()
                assertTrue(results.tryReceive().isFailure)
                otherLibraryState.value++
                val whileLoading = results.receive()
                assertEquals(customGroupId(1), whileLoading.groupId)
                assertEquals(listOf(shared, oldNext), whileLoading.tracks)

                queryResult.complete(listOf(shared, newNext))
                val newGroup = results.receive()
                assertEquals(customGroupId(2), newGroup.groupId)
                assertEquals(listOf(shared, newNext), newGroup.tracks)
                val nextIndex = deferredQueueStartIndex(newGroup.tracks.map(TrackEntity::uri), shared.uri)
                assertEquals(newNext, newGroup.tracks[nextIndex])
            } finally {
                collector.cancel()
            }
        }
    }

    @Test
    fun rapidSwitchesDiscardAnEarlierGroupsDelayedResult() = runBlocking {
        withTimeout(5_000) {
            val selected = MutableStateFlow<String?>(customGroupId(1))
            val delayedQueryStarted = CompletableDeferred<Unit>()
            val delayedQuery = CompletableDeferred<List<TrackEntity>>()
            val results = Channel<LibraryGroupTracks>(Channel.UNLIMITED)
            val collector = launch {
                observeGroupTracks(selected) { id ->
                    if (id == customGroupId(2)) {
                        flow {
                            delayedQueryStarted.complete(Unit)
                            emit(delayedQuery.await())
                        }
                    } else {
                        flowOf(listOf(track(id.orEmpty())))
                    }
                }.collect { results.send(it) }
            }
            try {
                results.receive()
                selected.value = customGroupId(2)
                delayedQueryStarted.await()
                selected.value = customGroupId(3)
                val latest = results.receive()
                assertEquals(customGroupId(3), latest.groupId)
                assertEquals(listOf(track(customGroupId(3))), latest.tracks)
                delayedQuery.complete(listOf(track("stale")))
                assertTrue(results.tryReceive().isFailure)
            } finally {
                collector.cancel()
            }
        }
    }

    @Test
    fun emptyGroupResultKeepsTheSelectedGroupIdentity() = runBlocking {
        withTimeout(5_000) {
            val selected = MutableStateFlow<String?>(null)
            val results = Channel<LibraryGroupTracks>(Channel.UNLIMITED)
            val collector = launch {
                observeGroupTracks(selected) { id ->
                    flowOf(if (id == null) listOf(track("all")) else emptyList())
                }.collect { results.send(it) }
            }
            try {
                assertEquals(null, results.receive().groupId)
                selected.value = customGroupId(2)
                val emptyGroup = results.receive()
                assertEquals(customGroupId(2), emptyGroup.groupId)
                assertTrue(emptyGroup.tracks.isEmpty())
            } finally {
                collector.cancel()
            }
        }
    }

    private fun track(id: String) = TrackEntity(
        uri = "file:///$id.mp3",
        rootUri = "file:///music",
        title = id,
        artist = "银临",
        album = "",
        durationMs = 0,
        mimeType = "audio/mpeg",
        lyricUri = null,
    )
}
