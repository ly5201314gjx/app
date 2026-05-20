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
        val sources = apiSourceDao.getAllSources().firstOrNull()
        if (sources.isNullOrEmpty()) {
            val defaultSource = ApiSource(
                url = "https://cj.lziapi.com/api.php/provide/vod/",
                name = "默认亮子极速",
                isDefault = true,
                isActive = true
            )
            apiSourceDao.insertSource(defaultSource)
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
        
        if (categoryId != null) {
            options["t"] = categoryId.toString()
        }
        
        if (!keyword.isNullOrBlank()) {
            options["wd"] = keyword.trim()
        }

        maccmsService.getVodData(baseUrl, options)
    }
}
