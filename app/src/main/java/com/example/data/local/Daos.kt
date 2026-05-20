package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteVod>>

    @Query("SELECT vodId FROM favorites")
    fun getAllFavoriteIds(): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(vod: FavoriteVod)

    @Query("DELETE FROM favorites WHERE vodId = :vodId")
    suspend fun deleteFavoriteById(vodId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE vodId = :vodId)")
    fun isFavoriteFlow(vodId: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE vodId = :vodId)")
    suspend fun isFavorite(vodId: Int): Boolean
}

@Dao
interface ApiSourceDao {
    @Query("SELECT * FROM api_sources ORDER BY isDefault DESC, name ASC")
    fun getAllSources(): Flow<List<ApiSource>>

    @Query("SELECT * FROM api_sources WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveSource(): ApiSource?

    @Query("SELECT * FROM api_sources WHERE isActive = 1 LIMIT 1")
    fun getActiveSourceFlow(): Flow<ApiSource?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: ApiSource)

    @Query("DELETE FROM api_sources WHERE url = :url AND isDefault = 0")
    suspend fun deleteSourceByUrl(url: String)

    @Query("UPDATE api_sources SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE api_sources SET isActive = 1 WHERE url = :url")
    suspend fun activateUrl(url: String)

    @Transaction
    suspend fun selectActiveSource(url: String) {
        deactivateAll()
        activateUrl(url)
    }
}
