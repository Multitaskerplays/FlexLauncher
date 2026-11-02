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
    val categoryId: Int = 0,
    val isSystemApp: Boolean = false,
    val isHidden: Boolean = false,
    val isCloned: Boolean = false,
    val clonedFromPackage: String? = null,
    val position: Int = 0,
    val customLabel: String? = null,
    val lastUsed: Long = 0,
    val installTime: Long = 0,
    val launchCount: Int = 0,
    val isFavorite: Boolean = false,
    val color: Int? = null,
    @Ignore
    val icon: Drawable? = null
) {
    constructor(
        packageName: String,
        appName: String,
        userId: Long,
        categoryId: Int = 0,
        isSystemApp: Boolean = false,
        isHidden: Boolean = false,
        isCloned: Boolean = false,
        clonedFromPackage: String? = null,
        position: Int = 0,
        customLabel: String? = null,
        lastUsed: Long = 0,
        installTime: Long = 0,
        launchCount: Int = 0,
        isFavorite: Boolean = false,
        color: Int? = null
    ) : this(
        packageName, appName, userId, categoryId, isSystemApp,
        isHidden, isCloned, clonedFromPackage, position, customLabel,
        lastUsed, installTime, launchCount, isFavorite, color, null
    )
}
