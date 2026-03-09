/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.domain.media.cache.storage

import me.him188.ani.app.domain.media.createTestDefaultMedia
import me.him188.ani.app.domain.media.createTestMediaProperties
import me.him188.ani.app.domain.media.resolver.EpisodeMetadata
import me.him188.ani.datasources.api.EpisodeSort
import me.him188.ani.datasources.api.MediaCacheMetadata
import me.him188.ani.datasources.api.topic.ResourceLocation
import me.him188.ani.utils.httpdownloader.MediaType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReadableMediaCacheNamingTest {
    private val media = createTestDefaultMedia(
        mediaId = "dmhy.10001",
        mediaSourceId = "dmhy",
        originalTitle = "test",
        download = ResourceLocation.MagnetLink("magnet:?xt=urn:btih:test"),
        originalUrl = "https://example.com/test",
        publishedTime = 0,
        properties = createTestMediaProperties(
            subtitleLanguageIds = listOf("CHS"),
            resolution = "1080P",
            alliance = "北宇治字幕组",
        ),
    )

    private val metadata = MediaCacheMetadata(
        subjectId = "1",
        episodeId = "2",
        subjectNameCN = "药屋少女的呢喃",
        subjectNames = listOf("药屋少女的呢喃"),
        episodeSort = EpisodeSort(3),
        episodeEp = EpisodeSort(3),
        episodeName = "第3话",
    )

    private val episodeMetadata = EpisodeMetadata(
        title = "第3话",
        ep = EpisodeSort(3),
        sort = EpisodeSort(3),
    )

    @Test
    fun `build http paths uses readable subject and file names`() {
        val paths = ReadableMediaCacheNaming.buildHttpSavePaths(
            media,
            metadata,
            episodeMetadata,
            MediaType.MP4,
        )

        assertEquals(
            "药屋少女的呢喃/药屋少女的呢喃_03_北宇治字幕组.mp4",
            paths.relativeOutputPath,
        )
        assertTrue(paths.relativeSegmentCacheDir.startsWith("药屋少女的呢喃/.ani-segments/药屋少女的呢喃_03_北宇治字幕组__"))
    }

    @Test
    fun `build torrent dir uses readable subject folder`() {
        val path = ReadableMediaCacheNaming.buildTorrentRelativeSaveDir(
            media,
            metadata,
            episodeMetadata,
        )

        assertTrue(path.startsWith("药屋少女的呢喃/药屋少女的呢喃_03_北宇治字幕组__"))
    }

    @Test
    fun `build completed file name keeps readable structure and extension`() {
        val name = ReadableMediaCacheNaming.buildReadableCompletedFileName(
            media,
            metadata,
            episodeMetadata,
            originalFileName = "raw-video.mkv",
        )

        assertEquals("药屋少女的呢喃_03_北宇治字幕组.mkv", name)
    }
}
