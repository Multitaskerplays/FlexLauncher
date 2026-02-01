package com.flexlauncher.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider

class ShizukuManager(private val context: Context) {
    
    private val onRequestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            val granted = grantResult == PackageManager.PERMISSION_GRANTED
            permissionCallback?.invoke(granted)
        }
    
    private var permissionCallback: ((Boolean) -> Unit)? = null
    
    init {
        Shizuku.addRequestPermissionResultListener(onRequestPermissionResultListener)
    }
    
    /**
     * Check if Shizuku is available and running
     */
    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if we have Shizuku permission
     */
    fun hasPermission(): Boolean {
        return if (isShizukuAvailable()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        } else {
            false
        }
    }
    
    /**
     * Request Shizuku permission
     */
    fun requestPermission(callback: (Boolean) -> Unit) {
        if (!isShizukuAvailable()) {
            callback(false)
            return
        }
        
        if (hasPermission()) {
            callback(true)
            return
        }
        
        permissionCallback = callback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
        }
    }
    
    /**
     * Clone an app for a specific user
     * This creates a separate instance of the app
     */
    suspend fun cloneApp(packageName: String, userId: Long): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!hasPermission()) {
                return@withContext Result.failure(SecurityException("Shizuku permission not granted"))
            }
            
            // Generate a unique package name for the clone
            val clonedPackageName = "${packageName}.clone${userId}"
            
            // Execute shell command to create work profile or clone
            val command = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Use Android's built-in app cloning on Android 9+
                "pm install-existing --user $userId $packageName"
            } else {
                // Fallback for older versions - this requires more complex implementation
                "pm path $packageName"
            }
            
            val result = executeShizukuCommand(command)
            
            if (result.isSuccess) {
                Result.success(clonedPackageName)
            } else {
                Result.failure(Exception("Failed to clone app: ${result.exceptionOrNull()?.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Remove a cloned app
     */
    suspend fun removeClonedApp(packageName: String, userId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!hasPermission()) {
                return@withContext Result.failure(SecurityException("Shizuku permission not granted"))
            }
            
            val command = "pm uninstall --user $userId $packageName"
            executeShizukuCommand(command)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Hide an app from the launcher
     */
    suspend fun hideApp(packageName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!hasPermission()) {
                return@withContext Result.failure(SecurityException("Shizuku permission not granted"))
            }
            
            val command = "pm disable-user --user 0 $packageName"
            executeShizukuCommand(command)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Unhide an app
     */
    suspend fun unhideApp(packageName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!hasPermission()) {
                return@withContext Result.failure(SecurityException("Shizuku permission not granted"))
            }
            
            val command = "pm enable $packageName"
            executeShizukuCommand(command)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get list of all installed packages
     */
    suspend fun getAllPackages(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (!hasPermission()) {
                return@withContext Result.failure(SecurityException("Shizuku permission not granted"))
            }
            
            val result = executeShizukuCommand("pm list packages")
            if (result.isSuccess) {
                val packages = result.getOrNull()
                    ?.split("\n")
                    ?.filter { it.startsWith("package:") }
                    ?.map { it.removePrefix("package:").trim() }
                    ?: emptyList()
                Result.success(packages)
            } else {
                Result.failure(Exception("Failed to list packages"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Execute a shell command via Shizuku
     */
    private suspend fun executeShizukuCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            
            process.waitFor()
            
            if (error.isNotEmpty()) {
                Result.failure(Exception(error))
            } else {
                Result.success(output)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun cleanup() {
        Shizuku.removeRequestPermissionResultListener(onRequestPermissionResultListener)
    }
    
    companion object {
        private const val SHIZUKU_PERMISSION_REQUEST_CODE = 1001
    }
}
