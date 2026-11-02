package com.flexlauncher.split

import android.content.Context

/** Remembers the last split so it can be re-launched in one tap. */
class SplitPrefs(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("flexlauncher_splits", Context.MODE_PRIVATE)

    var left: String?
        get() = prefs.getString(KEY_LEFT, null)
        set(value) = prefs.edit().putString(KEY_LEFT, value).apply()

    var right: String?
        get() = prefs.getString(KEY_RIGHT, null)
        set(value) = prefs.edit().putString(KEY_RIGHT, value).apply()

    var ratioPct: Int
        get() = prefs.getInt(KEY_RATIO, SplitGeometryCalculator.DEFAULT_RATIO_PCT)
        set(value) = prefs.edit().putInt(KEY_RATIO, value.coerceIn(20, 90)).apply()

    var bottomInsetPx: Int
        get() = prefs.getInt(KEY_BOTTOM, SplitGeometryCalculator.DEFAULT_BOTTOM_INSET_PX)
        set(value) = prefs.edit().putInt(KEY_BOTTOM, value).apply()

    var overscanPx: Int
        get() = prefs.getInt(KEY_OVERSCAN, SplitGeometryCalculator.DEFAULT_OVERSCAN_PX)
        set(value) = prefs.edit().putInt(KEY_OVERSCAN, value).apply()

    fun hasSavedPair(): Boolean = !left.isNullOrEmpty() && !right.isNullOrEmpty()

    fun savePair(leftPkg: String, rightPkg: String) {
        prefs.edit()
            .putString(KEY_LEFT, leftPkg)
            .putString(KEY_RIGHT, rightPkg)
            .apply()
    }

    private companion object {
        const val KEY_LEFT = "split_left"
        const val KEY_RIGHT = "split_right"
        const val KEY_RATIO = "split_ratio_pct"
        const val KEY_BOTTOM = "split_bottom_inset_px"
        const val KEY_OVERSCAN = "split_overscan_px"
    }
}
