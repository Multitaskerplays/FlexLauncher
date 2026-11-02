package com.flexlauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "launcher_settings")
data class LauncherSettings(
    @PrimaryKey
    val id: Int = 1,
    val isMultiUserMode: Boolean = false,
    val currentUserId: Long = 0,
    val lockOnScreenOff: Boolean = true,
    val showNotificationBadges: Boolean = true,
    val gridRows: Int = 5,
    val gridColumns: Int = 4,
    val iconSize: Float = 1.0f,
    val showAppLabels: Boolean = true,
    val enableGestures: Boolean = true,
    val searchProvider: String = "google",
    val wallpaperPath: String? = null,
    val enableBlur: Boolean = false,
    val blurRadius: Int = 25,
    val animationSpeed: Float = 1.0f,
    val hapticFeedback: Boolean = true,
    val showClock: Boolean = true,
    val clockFormat24h: Boolean = true,
    val showDate: Boolean = true,
    val showWeather: Boolean = true,
    val drawerStyle: String = "vertical", // vertical, horizontal, list
    val sortAppsBy: String = "name", // name, frequency, recent, custom
    val hideStatusBar: Boolean = false,
    val hideNavigationBar: Boolean = false,
    val doubleTabToSleep: Boolean = false,
    val swipeUpAction: String = "app_drawer", 
    val swipeDownAction: String = "notifications",
    val swipeLeftAction: String = "none",
    val swipeRightAction: String = "none",
    val enableFolders: Boolean = true,
    val backupEnabled: Boolean = true,
    val lastBackupTime: Long = 0
)
