/*
 * Copyright (C) 2024-2025 OpenAni and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
 *
 * https://github.com/open-ani/ani/blob/main/LICENSE
 */

package me.him188.ani.app.domain.media.cache.storage

import me.him188.ani.app.domain.media.resolver.EpisodeMetadata
import me.him188.ani.datasources.api.Media
import me.him188.ani.datasources.api.MediaCacheMetadata
import me.him188.ani.utils.httpdownloader.MediaType
import kotlin.math.absoluteValue

data class HttpCacheSavePaths(
    val relativeOutputPath: String,
    val relativeSegmentCacheDir: String,
)

object ReadableMediaCacheNaming {
    private val invalidPathCharsRegex = Regex("""[\\/:*?"<>|\u0000-\u001F]""")
    private val repeatedWhitespaceRegex = Regex("""\s+""")
    private val repeatedUnderscoreRegex = Regex("""_+""")

    fun buildHttpSavePaths(
        origin: Media,
        metadata: MediaCacheMetadata,
        episodeMetadata: EpisodeMetadata,
        mediaType: MediaType,
    ): HttpCacheSavePaths {
        val folderName = buildSubjectFolderName(metadata)
        val fileBaseName = buildReadableFileBaseName(origin, metadata, episodeMetadata)
        val shortHash = buildShortHash(origin, metadata)
        val extension = when (mediaType) {
            MediaType.M3U8 -> "ts"
            MediaType.MP4 -> "mp4"
            MediaType.MKV -> "mkv"
        }

        return HttpCacheSavePaths(
            relativeOutputPath = "$folderName/$fileBaseName.$extension",
            relativeSegmentCacheDir = "$folderName/.ani-segments/${fileBaseName}__${shortHash}",
        )
    }

    fun buildTorrentRelativeSaveDir(
        origin: Media,
        metadata: MediaCacheMetadata,
        episodeMetadata: EpisodeMetadata,
    ): String {
        val folderName = buildSubjectFolderName(metadata)
        val fileBaseName = buildReadableFileBaseName(origin, metadata, episodeMetadata)
        val shortHash = buildShortHash(origin, metadata)
        return "$folderName/${fileBaseName}__${shortHash}"
    }

    fun buildReadableCompletedFileName(
        origin: Media,
        metadata: MediaCacheMetadata,
        episodeMetadata: EpisodeMetadata,
        originalFileName: String,
    ): String {
        val extension = originalFileName.substringAfterLast('.', "")
            .takeIf { it.isNotBlank() }
        val fileBaseName = buildReadableFileBaseName(origin, metadata, episodeMetadata)
        return if (extension == null) {
            fileBaseName
        } else {
            "$fileBaseName.$extension"
        }
    }

    private fun buildReadableFileBaseName(
        origin: Media,
        metadata: MediaCacheMetadata,
        episodeMetadata: EpisodeMetadata,
    ): String {
        val subjectName = buildSubjectFolderName(metadata)
        val episodeName = sanitizePathSegment(
            (metadata.episodeEp ?: metadata.episodeSort).toString(),
            fallback = "EP",
            maxLength = 16,
        )
        val sourceName = origin.properties.alliance
            .trim()
            .takeIf { it.isNotEmpty() }
            ?.let { sanitizePathSegment(it, fallback = "", maxLength = 32) }
            ?.takeIf { it.isNotEmpty() }

        return listOf(subjectName, episodeName, sourceName)
            .filter { it.isNotBlank() }
            .joinToString("_")
            .let { sanitizePathSegment(it, fallback = episodeMetadata.sort.toString(), maxLength = 96) }
    }

    private fun buildSubjectFolderName(metadata: MediaCacheMetadata): String {
        return sanitizePathSegment(
            metadata.subjectNameCN
                ?.takeIf { it.isNotBlank() }
                ?: metadata.subjectNames.firstOrNull { it.isNotBlank() }
                ?: metadata.subjectId,
            fallback = metadata.subjectId,
            maxLength = 48,
        )
    }

    private fun buildShortHash(origin: Media, metadata: MediaCacheMetadata): String {
        return (origin.mediaId.hashCode() * 31
                + metadata.subjectId.hashCode() * 17
                + metadata.episodeId.hashCode())
            .absoluteValue
            .toString(16)
    }

    private fun sanitizePathSegment(
        value: String,
        fallback: String,
        maxLength: Int,
    ): String {
        val normalized = value.trim()
            .replace(invalidPathCharsRegex, "_")
            .replace(repeatedWhitespaceRegex, " ")
            .replace(repeatedUnderscoreRegex, "_")
            .trim(' ', '.', '_')
            .take(maxLength)

        if (normalized.isNotBlank()) {
            return normalized
        }

        return fallback.trim()
            .replace(invalidPathCharsRegex, "_")
            .replace(repeatedWhitespaceRegex, " ")
            .replace(repeatedUnderscoreRegex, "_")
            .trim(' ', '.', '_')
            .take(maxLength)
            .ifBlank { "cache" }
    }
}
