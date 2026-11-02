package com.flexlauncher.ui.main

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.LruCache

/**
 * Icons are a Drawable, so they cannot live in the Room entity (`AppInfo.icon` is @Ignore) -
 * they have to be loaded from the package manager whenever a row is bound. Cached because a
 * launcher binds the same package over and over while scrolling.
 */
class AppIcons(context: Context) {

    private val packageManager: PackageManager = context.applicationContext.packageManager
    private val cache = LruCache<String, Drawable?>(128)

    fun iconFor(packageName: String): Drawable? {
        cache.get(packageName)?.let { return it }
        val icon = try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        cache.put(packageName, icon)
        return icon
    }
}
