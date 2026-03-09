/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.domain.media.cache

import kotlinx.coroutines.test.runTest
import me.him188.ani.app.data.persistent.database.dao.TorrentCacheInfoEntity
import me.him188.ani.app.domain.media.resolver.EpisodeMetadata
import me.him188.ani.datasources.api.EpisodeSort
import me.him188.ani.datasources.api.MediaCacheMetadata
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TorrentMediaCacheEngineTest : AbstractTorrentMediaCacheEngineTest() {
    @Test
    fun `createCache persists readable relative dir without leading slash`() = runTest {
        val metadata = MediaCacheMetadata(
            subjectId = "subject-1",
            episodeId = "episode-2",
            subjectNameCN = "药屋少女的呢喃",
            subjectNames = listOf("The Apothecary Diaries"),
            episodeSort = EpisodeSort("03"),
            episodeEp = EpisodeSort("03"),
            episodeName = "第3话",
        )
        val episodeMetadata = EpisodeMetadata(
            title = "第3话",
            ep = EpisodeSort("03"),
            sort = EpisodeSort("03"),
        )
        val engine = createEngine(
            saveDirProvider = createReadableSaveDirProvider(),
            onDownloadStarted = {
                it.onTorrentChecked()
            },
        )

        engine.createCache(testMedia, metadata, episodeMetadata, coroutineContext)

        val entity = assertNotNull(torrentInfoDatabase.get(testMedia.mediaId))
        assertFalse(entity.relativeDir.startsWith("/"))
        assertTrue(entity.relativeDir.startsWith("pieces/药屋少女的呢喃/药屋少女的呢喃_03_"))
    }

    @Test
    fun `restore uses moved readable file outside pieces directory`() = runTest {
        val metadata = MediaCacheMetadata(
            subjectId = "subject-1",
            episodeId = "episode-2",
            subjectNameCN = "药屋少女的呢喃",
            subjectNames = listOf("The Apothecary Diaries"),
            episodeSort = EpisodeSort("03"),
            episodeEp = EpisodeSort("03"),
            episodeName = "第3话",
        )
        val targetFile = tempFile("药屋少女的呢喃", "药屋少女的呢喃_03_北宇治字幕组.mkv")
        targetFile.parentFile.mkdirs()
        targetFile.writeText("test-cache")

        torrentInfoDatabase.upsert(
            TorrentCacheInfoEntity(
                mediaId = testMedia.mediaId,
                torrentData = byteArrayOf(1, 2, 3),
                relativeDir = "pieces/药屋少女的呢喃/药屋少女的呢喃_03_hash",
                completed = true,
                pathInTorrent = "../../../药屋少女的呢喃/药屋少女的呢喃_03_北宇治字幕组.mkv",
            ),
        )

        val engine = createEngine(saveDirProvider = createReadableSaveDirProvider())
        val cache = engine.restore(testMedia, metadata, coroutineContext)

        val localCache = assertIs<LocalFileMediaCache>(cache)
        assertEquals(targetFile.absolutePath, localCache.file.absolutePath)
    }

}
