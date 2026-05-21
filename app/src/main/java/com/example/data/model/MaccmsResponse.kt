package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MaccmsResponse(
    @Json(name = "code") val code: Any? = null,
    @Json(name = "msg") val msg: String? = null,
    @Json(name = "page") val page: Any? = null,
    @Json(name = "pagecount") val pagecount: Any? = null,
    @Json(name = "limit") val limit: Any? = null,
    @Json(name = "total") val total: Any? = null,
    @Json(name = "list") val list: List<VodItem>? = null,
    @Json(name = "class") val classList: List<CategoryItem>? = null
)

@JsonClass(generateAdapter = true)
data class VodItem(
    @Json(name = "vod_id") val vodId: Int,
    @Json(name = "vod_name") val vodName: String,
    @Json(name = "type_id") val typeId: Any? = null,
    @Json(name = "type_name") val typeName: String? = null,
    @Json(name = "vod_pic") val vodPic: String? = null,
    @Json(name = "vod_remarks") val vodRemarks: String? = null,
    @Json(name = "vod_actor") val vodActor: String? = null,
    @Json(name = "vod_director") val vodDirector: String? = null,
    @Json(name = "vod_content") val vodContent: String? = null,
    @Json(name = "vod_play_from") val vodPlayFrom: String? = null,
    @Json(name = "vod_play_url") val vodPlayUrl: String? = null,
    @Json(name = "vod_sub") val vodSub: String? = null,
    @Json(name = "vod_year") val vodYear: String? = null,
    @Json(name = "vod_area") val vodArea: String? = null,
    @Transient var apiSourceUrl: String? = null,
    @Transient var apiSourceName: String? = null
) {
    // Utility to parse play URLs
    // e.g. "第1集$http://link1#第2集$http://link2"
    fun getPlayEpisodes(): List<PlayEpisode> {
        val playUrlStr = vodPlayUrl ?: return emptyList()
        val playFromStr = vodPlayFrom ?: "播放源1"
        
        // Multi-source can exist: separated by $$$
        val sources = playUrlStr.split("$$$")
        val sourceNames = playFromStr.split("$$$")
        
        val list = mutableListOf<PlayEpisode>()
        
        for (i in sources.indices) {
            val srcUrlString = sources[i]
            val srcName = sourceNames.getOrNull(i) ?: "播放源${i + 1}"
            
            // Filter out problematic sources
            if (srcName.contains("feifan", ignoreCase = true) || 
                srcName.contains("liangzi", ignoreCase = true)) continue
            
            // Episodes are separated by #
            val episodes = srcUrlString.split("#")
            for (ep in episodes) {
                if (ep.isBlank()) continue
                // Each episode is "name$url"
                val parts = ep.split("$")
                if (parts.size >= 2) {
                    val name = parts[0].trim()
                    val url = parts.subList(1, parts.size).joinToString("$").trim()
                    list.add(PlayEpisode(sourceName = srcName, name = name, url = url))
                } else if (ep.isNotBlank()) {
                    list.add(PlayEpisode(sourceName = srcName, name = "播放", url = ep.trim()))
                }
            }
        }
        return list
    }
}

data class PlayEpisode(
    val sourceName: String,
    val name: String,
    val url: String
)

@JsonClass(generateAdapter = true)
data class CategoryItem(
    @Json(name = "type_id") val typeId: Int,
    @Json(name = "type_name") val typeName: String
)
