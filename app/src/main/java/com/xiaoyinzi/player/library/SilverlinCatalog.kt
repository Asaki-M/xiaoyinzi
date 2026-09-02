package com.xiaoyinzi.player.library

import com.xiaoyinzi.player.data.TrackEntity
import java.util.Locale

data class PresetAlbum(
    val id: String,
    val title: String,
    val year: Int,
    val tracks: List<String>,
)

data class CatalogTrackMatch(
    val title: String,
    val track: TrackEntity?,
)

object SilverlinCatalog {
    val albums: List<PresetAlbum> = listOf(
        PresetAlbum(
            id = "fu-cao-wei-ying",
            title = "腐草为萤",
            year = 2013,
            tracks = listOf(
                "浮生辞",
                "泸沽寻梦",
                "Zodiac",
                "棠梨煎雪",
                "故城",
                "洒拓歌",
                "落梅笺",
                "情囚",
                "Backseat Stargazer",
                "锦鲤抄",
                "腐草为萤",
            ),
        ),
        PresetAlbum(
            id = "pi-fu-du-hai",
            title = "蚍蜉渡海",
            year = 2017,
            tracks = listOf(
                "灼",
                "裁梦为魂",
                "春笺",
                "不离",
                "如一",
                "不老梦",
                "青山揽梦",
                "是风动",
                "秋水",
                "卑微情书",
                "说余梦",
            ),
        ),
        PresetAlbum(
            id = "liu-li",
            title = "琉璃",
            year = 2020,
            tracks = listOf(
                "亲爱的瑞秋",
                "琉璃",
                "玫瑰与泪",
                "无际涯",
                "美人灯",
                "西施江南",
                "海棠春睡",
                "迟迟",
                "记忘歌",
                "白噪音",
                "无人生还",
                "终身成就",
            ),
        ),
        PresetAlbum(
            id = "li-di-shi-gong-fen-b-mian",
            title = "离地十公分·B面",
            year = 2022,
            tracks = listOf(
                "窗前明月光",
                "见夏如晤",
                "日出前起飞",
                "Drive Until Sunset",
            ),
        ),
        PresetAlbum(
            id = "shan-se-you-wu-zhong",
            title = "山色有无中",
            year = 2025,
            tracks = listOf(
                "沧海飞尘",
                "愚人歌",
                "少女庄周",
                "你奔向春野",
                "眠花去",
                "魄心",
                "折柳记",
                "山色有无中",
                "夜国",
                "我生于野",
                "幻海同游",
                "春归",
            ),
        ),
        PresetAlbum(
            id = "lin-lin",
            title = "粼粼",
            year = 2025,
            tracks = listOf(
                "粼粼",
                "断尾",
                "碧溪水",
                "天和山雨雪",
            ),
        ),
    )

    fun matchAlbum(album: PresetAlbum, localTracks: List<TrackEntity>): List<CatalogTrackMatch> {
        val localByTitle = localTracks.groupBy { normalizedTrackTitle(it.title) }
        return album.tracks.map { title ->
            CatalogTrackMatch(
                title = title,
                track = localByTitle[normalizedTrackTitle(title)]?.firstOrNull(),
            )
        }
    }

}

internal fun normalizedTrackTitle(title: String): String = title
    .trim()
    .replace(Regex("^\\d{1,3}[\\s._、-]+"), "")
    .replace(Regex("[（(【\\[].*?[）)】\\]]"), "")
    .lowercase(Locale.ROOT)
    .filter(Char::isLetterOrDigit)

fun albumGroupId(albumId: String): String = "preset:album:$albumId"

fun customGroupId(groupId: Long): String = "custom:$groupId"

fun String.customGroupIdOrNull(): Long? = takeIf { it.startsWith("custom:") }
    ?.removePrefix("custom:")
    ?.toLongOrNull()
