package com.flexlauncher.split

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.flexlauncher.FlexLauncherApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Puts two apps side by side as freeform windows.
 *
 * Why freeform and not the platform/stock split?
 *
 *  - A third-party launcher cannot use the stock split on these car boxes: the vendor's
 *    "split screen" shortcut only broadcasts an intent that the *stock* launcher3 activity
 *    receives. When FlexLauncher is HOME, launcher3 never runs, so that broadcast is a
 *    silent no-op.
 *  - Freeform windows are positioned by us: `windowingMode=5` plus explicit
 *    `launchBounds` gives exact pixel control, which is what a car's fixed card needs.
 *
 * The only prerequisite is the `enable_freeform_support` setting. This class prefers to
 * write it as the launcher itself (needs WRITE_SECURE_SETTINGS, grantable with
 * `adb shell pm grant com.flexlauncher android.permission.WRITE_SECURE_SETTINGS`) and
 * falls back to Shizuku's shell if that permission is missing.
 */
object SplitLauncher {

    private const val TAG = "FlexSplit"
    private const val WINDOWING_MODE_FREEFORM = 5
    private const val KEY_WINDOWING_MODE = "android.activity.windowingMode"
    private const val KEY_LAUNCH_BOUNDS = "android:activity.launchBounds"
    private const val FREEFORM_SETTING = "enable_freeform_support"

    /** SystemUI draws window decorations when it *starts* with freeform on, so undo it. */
    private const val RESTORE_DELAY_MS = 4_000L

    fun isFreeformEnabled(context: Context): Boolean = try {
        Settings.Global.getInt(context.contentResolver, FREEFORM_SETTING, 0) == 1
    } catch (t: Throwable) {
        false
    }

    /**
     * Launches [leftPackage] and [rightPackage] side by side.
     *
     * [onResult] runs on the main thread with `(launched, message)`.
     */
    fun start(
        activity: Activity,
        leftPackage: String,
        rightPackage: String,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        val prefs = SplitPrefs(activity)
        val geometry = SplitGeometryCalculator.forActivity(
            activity,
            prefs.ratioPct,
            prefs.bottomInsetPx,
            prefs.overscanPx
        )

        if (isFreeformEnabled(activity)) {
            val ok = launchPair(activity, leftPackage, rightPackage, geometry)
            onResult?.invoke(ok, null)
            return
        }

        if (writeFreeform(activity, true)) {
            val ok = launchPair(activity, leftPackage, rightPackage, geometry)
            restoreFreeformLater(activity)
            onResult?.invoke(ok, null)
            return
        }

        // No WRITE_SECURE_SETTINGS: ask Shizuku's shell instead.
        val shizuku = runCatching { FlexLauncherApp.instance.shizukuManager }.getOrNull()
        if (shizuku != null && shizuku.isShizukuAvailable() && shizuku.hasPermission()) {
            kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
                val result = shizuku.runShell("settings put global $FREEFORM_SETTING 1")
                if (result.isFailure || !isFreeformEnabled(activity)) {
                    onResult?.invoke(false, "Shizuku could not enable freeform support")
                    return@launch
                }
                val ok = launchPair(activity, leftPackage, rightPackage, geometry)
                restoreFreeformLater(activity)
                onResult?.invoke(ok, null)
            }
            return
        }

        onResult?.invoke(
            false,
            "Split screen needs freeform support. Grant it once with " +
                "\"adb shell pm grant ${activity.packageName} android.permission.WRITE_SECURE_SETTINGS\" " +
                "or install Shizuku."
        )
    }

    /** Same as [start] but for the pair remembered in [SplitPrefs]. */
    fun repeatLast(activity: Activity, onResult: ((Boolean, String?) -> Unit)? = null): Boolean {
        val prefs = SplitPrefs(activity)
        val left = prefs.left
        val right = prefs.right
        if (left.isNullOrEmpty() || right.isNullOrEmpty()) return false
        start(activity, left, right, onResult)
        return true
    }

    /** True when the given packages still resolve to launchable apps. */
    fun canLaunch(context: Context, packageName: String): Boolean =
        context.packageManager.getLaunchIntentForPackage(packageName) != null

    private fun launchPair(
        activity: Activity,
        leftPackage: String,
        rightPackage: String,
        geometry: SplitGeometry
    ): Boolean {
        val leftOk = launchOne(activity, leftPackage, geometry.left.toRect())
        val rightOk = launchOne(activity, rightPackage, geometry.right.toRect())
        if (leftOk && rightOk) {
            SplitPrefs(activity).savePair(leftPackage, rightPackage)
        } else if (!leftOk) {
            Log.w(TAG, "left pane failed: $leftPackage")
        } else {
            Log.w(TAG, "right pane failed: $rightPackage")
        }
        Log.i(
            TAG,
            "split ${geometry.appWidth}x${geometry.appHeight} divider=${geometry.dividerPx} " +
                "L=$leftPackage $leftOk ${geometry.left} R=$rightPackage $rightOk ${geometry.right}"
        )
        return leftOk && rightOk
    }

    private fun launchOne(activity: Activity, packageName: String, bounds: Rect): Boolean {
        val intent = activity.packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            Log.w(TAG, "no launch intent for $packageName")
            return false
        }
        // Note: launchBounds only take effect when the task is created. Re-running a split
        // over a pane that is *already open* leaves that window where it is - freeform
        // tasks are not resized by a later launch (verified on the box, with and without
        // CLEAR_TASK). The picker says so; closing the app gives it the new divider.
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        val options = ActivityOptions.makeBasic().toBundle().apply {
            putInt(KEY_WINDOWING_MODE, WINDOWING_MODE_FREEFORM)
            putParcelable(KEY_LAUNCH_BOUNDS, bounds)
        }
        return try {
            activity.startActivity(intent, options)
            true
        } catch (t: Throwable) {
            Log.e(TAG, "launch failed for $packageName", t)
            false
        }
    }

    private fun writeFreeform(context: Context, enabled: Boolean): Boolean = try {
        Settings.Global.putInt(context.contentResolver, FREEFORM_SETTING, if (enabled) 1 else 0)
        isFreeformEnabled(context) == enabled
    } catch (t: Throwable) {
        Log.i(TAG, "cannot write $FREEFORM_SETTING: $t")
        false
    }

    private fun restoreFreeformLater(activity: Activity) {
        val app = activity.applicationContext
        Handler(Looper.getMainLooper()).postDelayed({
            writeFreeform(app, false)
        }, RESTORE_DELAY_MS)
    }
}
