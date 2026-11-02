package com.flexlauncher.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.flexlauncher.FlexLauncherApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Refreshes the stored app list when packages are installed, replaced or removed.
 *
 * Without this the grid only ever shows whatever existed when setup ran, which is why new
 * apps used to be invisible until the profile was emptied and re-synced.
 */
class PackageChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_PACKAGE_ADDED &&
            action != Intent.ACTION_PACKAGE_REMOVED &&
            action != Intent.ACTION_PACKAGE_CHANGED &&
            action != Intent.ACTION_PACKAGE_REPLACED
        ) {
            return
        }

        val app = context.applicationContext as? FlexLauncherApp ?: return
        val changedPackage = intent.data?.schemeSpecificPart
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                syncCurrentProfile(app)
                Log.i(TAG, "$changedPackage -> resynced")
            } catch (t: Throwable) {
                Log.w(TAG, "resync failed", t)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "FlexLauncher"

        /** Syncs the selected profile, or the default one before a profile is chosen. */
        suspend fun syncCurrentProfile(app: FlexLauncherApp) {
            val selected = app.database.settingsDao().getSettingsOnce()?.currentUserId ?: 0L
            val userId = if (selected != 0L) selected else app.database.userDao().getDefaultUser()?.id
            if (userId == null) return
            app.appRepository.syncApps(userId)
            app.appRepository.pruneMissingApps(userId)
        }
    }
}
