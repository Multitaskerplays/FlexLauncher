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
    
    fun getAppsByCategory(userId: Long, categoryId: Int): LiveData<List<AppInfo>> {
        return appDao.getAppsByCategory(userId, categoryId)
    }
    
    fun getFavoriteApps(userId: Long): LiveData<List<AppInfo>> {
        return appDao.getFavoriteApps(userId)
    }
    
    fun getRecentApps(userId: Long, limit: Int = 10): LiveData<List<AppInfo>> {
        return appDao.getRecentApps(userId, limit)
    }
    
    fun getMostUsedApps(userId: Long, limit: Int = 10): LiveData<List<AppInfo>> {
        return appDao.getMostUsedApps(userId, limit)
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
    
    /**
     * Clone an app using Shizuku
     */
    suspend fun cloneApp(packageName: String, userId: Long): Result<AppInfo> = withContext(Dispatchers.IO) {
        try {
            val result = shizukuManager.cloneApp(packageName, userId)
            
            if (result.isSuccess) {
                val clonedPackageName = result.getOrNull() ?: return@withContext Result.failure(Exception("No package name returned"))
                
                // Get original app info
                val originalApp = appDao.getApp(packageName, userId)
                    ?: return@withContext Result.failure(Exception("Original app not found"))
                
                // Create cloned app entry
                val clonedApp = originalApp.copy(
                    packageName = clonedPackageName,
                    isCloned = true,
                    clonedFromPackage = packageName,
                    customLabel = "${originalApp.appName} (Clone)"
                )
                
                appDao.insertApp(clonedApp)
                Result.success(clonedApp)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Remove a cloned app
     */
    suspend fun removeClonedApp(packageName: String, userId: Long): Result<Unit> {
        return try {
            val result = shizukuManager.removeClonedApp(packageName, userId)
            if (result.isSuccess) {
                appDao.deleteAppByPackage(packageName)
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Toggle app favorite status
     */
    suspend fun toggleFavorite(packageName: String, userId: Long) {
        val app = appDao.getApp(packageName, userId)
        app?.let {
            val updated = it.copy(isFavorite = !it.isFavorite)
            appDao.updateApp(updated)
        }
    }
    
    /**
     * Hide/unhide an app
     */
    suspend fun toggleHidden(packageName: String, userId: Long) {
        val app = appDao.getApp(packageName, userId)
        app?.let {
            val updated = it.copy(isHidden = !it.isHidden)
            appDao.updateApp(updated)
            
            // Use Shizuku to actually hide the app if needed
            if (updated.isHidden) {
                shizukuManager.hideApp(packageName)
            } else {
                shizukuManager.unhideApp(packageName)
            }
        }
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
