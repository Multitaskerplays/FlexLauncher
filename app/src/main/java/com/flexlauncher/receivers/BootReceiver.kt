package com.flexlauncher.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.flexlauncher.FlexLauncherApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Re-reads the installed apps after a reboot so the grid matches the device. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        val app = context.applicationContext as? FlexLauncherApp ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                PackageChangeReceiver.syncCurrentProfile(app)
                Log.i(TAG, "boot sync done")
            } catch (t: Throwable) {
                Log.w(TAG, "boot sync failed", t)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "FlexLauncher"
    }
}
