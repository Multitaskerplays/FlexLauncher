package com.flexlauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "launcher_settings")
data class LauncherSettings(
    @PrimaryKey
    val id: Int = 1,
    val isMultiUserMode: Boolean = false,
    val currentUserId: Long = 0,
    val showClock: Boolean = true,
    val clockFormat24h: Boolean = true,
    val showAppLabels: Boolean = true
)
