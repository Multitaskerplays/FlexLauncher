package com.flexlauncher.data.model

import android.graphics.drawable.Drawable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "apps")
data class AppInfo(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val userId: Long,
    val isSystemApp: Boolean = false,
    val isHidden: Boolean = false,
    val position: Int = 0,
    val customLabel: String? = null,
    val lastUsed: Long = 0,
    val installTime: Long = 0,
    val launchCount: Int = 0,
    @Ignore
    val icon: Drawable? = null
) {
    /** Room needs a constructor that matches only the persisted columns. */
    constructor(
        packageName: String,
        appName: String,
        userId: Long,
        isSystemApp: Boolean,
        isHidden: Boolean,
        position: Int,
        customLabel: String?,
        lastUsed: Long,
        installTime: Long,
        launchCount: Int
    ) : this(
        packageName, appName, userId, isSystemApp, isHidden,
        position, customLabel, lastUsed, installTime, launchCount, null
    )
}
