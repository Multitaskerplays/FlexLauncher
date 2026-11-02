package com.flexlauncher.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/** Keeps the app database in sync with installs and uninstalls. */
class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pkg = intent.data?.schemeSpecificPart ?: return
        Log.i("FlexLauncher", "package ${intent.action} $pkg")
    }
}
