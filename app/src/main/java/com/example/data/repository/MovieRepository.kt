package com.example.data.repository

import com.example.data.api.MaccmsService
import com.example.data.local.ApiSource
import com.example.data.local.ApiSourceDao
import com.example.data.local.FavoriteDao
import com.example.data.local.FavoriteVod
import com.example.data.model.MaccmsResponse
import com.example.data.model.VodItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class MovieRepository(
    private val favoriteDao: FavoriteDao,
    private val apiSourceDao: ApiSourceDao,
    private val maccmsService: MaccmsService
) {
    // Favorites Streams & Operations
    val allFavorites: Flow<List<FavoriteVod>> = favoriteDao.getAllFavorites()
    val allFavoriteIds: Flow<List<Int>> = favoriteDao.getAllFavoriteIds()

    suspend fun addFavorite(vod: VodItem, sourceUrl: String) = withContext(Dispatchers.IO) {
        val favorite = FavoriteVod(
            vodId = vod.vodId,
            vodName = vod.vodName,
            vodPic = vod.vodPic,
            vodRemarks = vod.vodRemarks,
            typeName = vod.typeName,
            apiSourceUrl = sourceUrl,
            timestamp = System.currentTimeMillis()
        )
        favoriteDao.insertFavorite(favorite)
    }

    suspend fun removeFavorite(vodId: Int) = withContext(Dispatchers.IO) {
        favoriteDao.deleteFavoriteById(vodId)
    }

    fun isFavoriteFlow(vodId: Int): Flow<Boolean> = favoriteDao.isFavoriteFlow(vodId)
    suspend fun isFavorite(vodId: Int): Boolean = withContext(Dispatchers.IO) {
        favoriteDao.isFavorite(vodId)
    }

    // API Source Streams & Operations
    val allSources: Flow<List<ApiSource>> = apiSourceDao.getAllSources()
    val activeSourceFlow: Flow<ApiSource?> = apiSourceDao.getActiveSourceFlow()

    suspend fun ensureDefaultSource() = withContext(Dispatchers.IO) {
        val sources = apiSourceDao.getAllSources().firstOrNull() ?: emptyList()
        val defaultUrls = listOf(
            "https://cj.lziapi.com/api.php/provide/vod/",
            "https://cj.ffzyapi.com/api.php/provide/vod/",
            "https://suoniapi.com/api.php/provide/vod/",
            "https://cj.jyzyapi.com/api.php/provide/vod/",
            "https://cj.speedbyg.com/api.php/provide/vod/"
        )
        val missingUrls = defaultUrls.filter { url -> sources.none { it.url == url } }
        if (missingUrls.isNotEmpty()) {
            val defaults = listOf(
                ApiSource("https://cj.lziapi.com/api.php/provide/vod/", "1号 极速秒播专线", isDefault = true, isActive = true),
                ApiSource("https://cj.ffzyapi.com/api.php/provide/vod/", "2号 非凡高清专线", isDefault = true, isActive = false),
                ApiSource("https://suoniapi.com/api.php/provide/vod/", "3号 索尼臻彩专线", isDefault = true, isActive = false),
                ApiSource("https://cj.jyzyapi.com/api.php/provide/vod/", "4号 金鹰优质专线", isDefault = true, isActive = false),
                ApiSource("https://cj.speedbyg.com/api.php/provide/vod/", "5号 快播硬核专线", isDefault = true, isActive = false)
            )
            val toInsert = defaults.filter { missingUrls.contains(it.url) }
            val activeExists = sources.any { it.isActive }
            
            toInsert.forEach { source ->
                val finalSource = if (activeExists && source.isActive) {
                    source.copy(isActive = false)
                } else {
                    source
                }
                apiSourceDao.insertSource(finalSource)
            }
        }
    }

    suspend fun getActiveSource(): ApiSource? = withContext(Dispatchers.IO) {
        apiSourceDao.getActiveSource()
    }

    suspend fun selectSource(url: String) = withContext(Dispatchers.IO) {
        apiSourceDao.selectActiveSource(url)
    }

    suspend fun addCustomSource(name: String, url: String) = withContext(Dispatchers.IO) {
        // Normalise URL to make sure it includes http/https
        val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }
        val source = ApiSource(
            url = formattedUrl,
            name = name,
            isDefault = false,
            isActive = false
        )
        apiSourceDao.insertSource(source)
    }

    suspend fun deleteSource(url: String) = withContext(Dispatchers.IO) {
        apiSourceDao.deleteSourceByUrl(url)
        // If the active source was deleted, reactivate the default source
        val active = apiSourceDao.getActiveSource()
        if (active == null) {
            val sources = apiSourceDao.getAllSources().firstOrNull()
            val backup = sources?.find { it.isDefault } ?: sources?.firstOrNull()
            if (backup != null) {
                apiSourceDao.selectActiveSource(backup.url)
            }
        }
    }

    // Fetch from Web Api
    suspend fun fetchVodList(
        baseUrl: String,
        pg: Int,
        categoryId: Int? = null,
        keyword: String? = null
    ): MaccmsResponse = withContext(Dispatchers.IO) {
        val options = mutableMapOf<String, String>()
        options["ac"] = "detail"
        options["pg"] = pg.toString()
        options["pagesize"] = "40"
        options["limit"] = "40"
        
        if (categoryId != null) {
            options["t"] = categoryId.toString()
        }
        
        if (!keyword.isNullOrBlank()) {
            options["wd"] = keyword.trim()
        }

        maccmsService.getVodData(baseUrl, options)
    }

    suspend fun fetchVodDetails(
        baseUrl: String,
        vodId: Int
    ): MaccmsResponse = withContext(Dispatchers.IO) {
        val options = mutableMapOf<String, String>()
        options["ac"] = "detail"
        options["ids"] = vodId.toString()
        maccmsService.getVodData(baseUrl, options)
    }
}
