package com.flexlauncher.split

import android.app.Activity
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics

/**
 * Pixel maths for a two-pane split.
 *
 * Freeform windows are positioned in *display* pixels. Launchers that cover the whole
 * screen (including this one) have a window that is the app area, so its metrics are the
 * right coordinate space for both panes — except on boxes that reserve a strip for a
 * vendor status bar, where the app area is narrower than the panel. Split Deck hit exactly
 * that on the Carlinkit box: 2374 px panel, 2270 px app area. Only the app area matters
 * for freeform, so we read the window, never the panel.
 */
data class SplitGeometry(
    val appWidth: Int,
    val appHeight: Int,
    val dividerPx: Int,
    val left: Pane,
    val right: Pane
)

/**
 * A pane rectangle in plain integers, so the maths can be unit-tested on the JVM without
 * the Android framework (android.graphics.Rect is a stub in local unit tests).
 */
data class Pane(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    fun toRect(): Rect = Rect(left, top, right, bottom)
}

object SplitGeometryCalculator {

    /**
     * Defaults measured on the Carlinkit Ultra AI Box behind an XPeng G6. The percentage is
     * the share of the **display panel** - like the vendor's `persist.splitscreen.percent`
     * and unlike the app area - because that is the number that describes where the car's
     * CarPlay card edge is (63:37 of 2374 px = 1495 px, the card edge).
     */
    const val DEFAULT_RATIO_PCT = 63
    const val DEFAULT_BOTTOM_INSET_PX = 45
    const val DEFAULT_OVERSCAN_PX = 10

    /**
     * The area an app may draw in, which is *not* the whole display on these boxes.
     *
     * `currentWindowMetrics.bounds` reports the full 2374 px panel here — it includes the
     * vendor's 104 px status strip, because the window is allowed to extend under it. The
     * app area (2270 px) is what `DisplayMetrics` reports, and it is also what freeform
     * bounds are clamped to.
     */
    fun appArea(activity: Activity): Pair<Int, Int> {
        val metrics = activity.resources.displayMetrics
        if (metrics.widthPixels > 0 && metrics.heightPixels > 0) {
            return metrics.widthPixels to metrics.heightPixels
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = activity.windowManager.currentWindowMetrics.bounds
            if (bounds.width() > 0 && bounds.height() > 0) {
                return bounds.width() to bounds.height()
            }
        }
        return 2270 to 1016
    }

    /** Width of the whole display panel, strip included; what the percentage refers to. */
    fun panelWidth(activity: Activity): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = activity.windowManager.currentWindowMetrics.bounds
            if (bounds.width() > 0) return bounds.width()
        }
        return appArea(activity).first
    }

    fun forActivity(
        activity: Activity,
        ratioPct: Int,
        bottomInsetPx: Int,
        overscanPx: Int
    ): SplitGeometry {
        val (width, height) = appArea(activity)
        val panel = panelWidth(activity).coerceAtLeast(width)
        val divider = (panel * ratioPct.coerceIn(20, 90) / 100).coerceIn(1, width - 1)
        return bounds(width, height, divider, bottomInsetPx, overscanPx)
    }

    /**
     * Pure bounds maths so it can be unit-tested without an Activity.
     *
     * [dividerPx] is the divider position in app-area pixels (already derived from the
     * panel percentage). The panes overflow by [overscanPx] on the outside edges so a car's
     * projection window does not show a wallpaper border, and stop [bottomInsetPx] above
     * the bottom edge because the car's dock lives there.
     */
    fun bounds(
        width: Int,
        height: Int,
        dividerPx: Int,
        bottomInsetPx: Int,
        overscanPx: Int
    ): SplitGeometry {
        val divider = dividerPx.coerceIn(1, width - 1)
        // No negative top: the window manager shifts a freeform rect downwards when its top
        // would be above the screen, which silently moves the pane off the intended edge.
        val top = 0
        val bottom = height - bottomInsetPx
        return SplitGeometry(
            appWidth = width,
            appHeight = height,
            dividerPx = divider,
            left = Pane(-overscanPx, top, divider, bottom),
            right = Pane(divider, top, width + overscanPx, bottom)
        )
    }
}
