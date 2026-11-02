package com.flexlauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val passwordHash: String,
    val photoPath: String? = null,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val themeId: Int = 0,
    val accentColor: Int = 0xFF6200EE.toInt(),
    val useBiometric: Boolean = false,
    val iconPackPackageName: String? = null
)
