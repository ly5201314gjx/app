package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteVod(
    @PrimaryKey val vodId: Int,
    val vodName: String,
    val vodPic: String?,
    val vodRemarks: String?,
    val typeName: String?,
    val apiSourceUrl: String, // Marks which API source this favorite belongs to
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "api_sources")
data class ApiSource(
    @PrimaryKey val url: String,
    val name: String,
    val isDefault: Boolean = false,
    val isActive: Boolean = false
)
