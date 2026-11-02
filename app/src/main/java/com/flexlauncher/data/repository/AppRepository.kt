package com.flexlauncher.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.LiveData
import com.flexlauncher.data.dao.AppDao
import com.flexlauncher.data.model.AppInfo
import com.flexlauncher.shizuku.ShizukuManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppRepository(
    private val context: Context,
    private val appDao: AppDao,
    private val shizukuManager: ShizukuManager
) {
    
    private val packageManager: PackageManager = context.packageManager
    
    fun getAppsForUser(userId: Long): LiveData<List<AppInfo>> {
        return appDao.getAppsForUser(userId)
    }
    
    suspend fun getApp(packageName: String, userId: Long): AppInfo? {
        return appDao.getApp(packageName, userId)
    }
    
    /**
     * Load all installed apps for a user
     */
    suspend fun loadInstalledApps(userId: Long): List<AppInfo> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        
        val apps = packageManager.queryIntentActivities(intent, 0)
        val appInfoList = mutableListOf<AppInfo>()
        
        for (resolveInfo in apps) {
            val activityInfo = resolveInfo.activityInfo
            val packageName = activityInfo.packageName
            val appName = activityInfo.loadLabel(packageManager).toString()
            val isSystemApp = (activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            
            try {
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                val installTime = packageInfo.firstInstallTime
                
                val appInfo = AppInfo(
                    packageName = packageName,
                    appName = appName,
                    userId = userId,
                    isSystemApp = isSystemApp,
                    installTime = installTime,
                    icon = activityInfo.loadIcon(packageManager)
                )
                
                appInfoList.add(appInfo)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        appInfoList
    }
    
    /**
     * Sync apps with database
     */
    suspend fun syncApps(userId: Long) {
        val installedApps = loadInstalledApps(userId)
        appDao.insertApps(installedApps)
    }

    /** Drops rows for packages that are no longer installed. */
    suspend fun pruneMissingApps(userId: Long) {
        val installed = packageManager
            .queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.activityInfo.packageName }
            .toSet()
        appDao.getAppsForUserOnce(userId)
            .filterNot { it.packageName in installed }
            .forEach { appDao.deleteApp(it) }
    }
    
    /**
     * Hide/unhide an app in this launcher. Deliberately a launcher-local flag: `pm
     * disable-user` would remove the app for everyone and needs Shizuku.
     */
    suspend fun toggleHidden(packageName: String, userId: Long) {
        val app = appDao.getApp(packageName, userId)
        app?.let { appDao.updateApp(it.copy(isHidden = !it.isHidden)) }
    }
    
    /**
     * Update app usage statistics
     */
    suspend fun updateAppUsage(packageName: String, userId: Long) {
        appDao.updateAppUsage(packageName, userId, System.currentTimeMillis())
    }
    
    /**
     * Launch an app
     */
    fun launchApp(packageName: String, userId: Long) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                // Update usage in background
                kotlinx.coroutines.GlobalScope.launch {
                    updateAppUsage(packageName, userId)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Uninstall an app
     */
    fun uninstallApp(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE)
        intent.data = android.net.Uri.parse("package:$packageName")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}
