package com.flexlauncher.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku
import java.io.FileInputStream

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
     * Run a raw shell command through Shizuku (shell UID), e.g. flipping a global setting
     * that would otherwise need WRITE_SECURE_SETTINGS.
     */
    suspend fun runShell(command: String): Result<String> = executeShizukuCommand(command)

    /**
     * Execute a shell command via Shizuku
     */
    private suspend fun executeShizukuCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        val process = try {
            // rikka.shizuku 13.1.5 has no Shizuku.newProcess(); go through the service
            // interface, whose IRemoteProcess exposes the three streams as file descriptors.
            val service = IShizukuService.Stub.asInterface(Shizuku.getBinder())
                ?: return@withContext Result.failure(Exception("Shizuku service is not available"))
            service.newProcess(arrayOf("sh", "-c", command), null, null)
                ?: return@withContext Result.failure(Exception("Shizuku could not start the command"))
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }

        try {
            // Reading the pipes before waitFor() blocks forever if the command never exits,
            // so bound the whole exchange and kill the process afterwards either way.
            withTimeoutOrNull(SHELL_TIMEOUT_MS) {
                val output = FileInputStream(process.inputStream.fileDescriptor)
                    .bufferedReader().use { it.readText() }
                val error = FileInputStream(process.errorStream.fileDescriptor)
                    .bufferedReader().use { it.readText() }
                process.waitFor()

                if (error.isNotBlank()) {
                    Result.failure(Exception(error.trim()))
                } else {
                    Result.success(output)
                }
            } ?: Result.failure(Exception("Shizuku command timed out after ${SHELL_TIMEOUT_MS} ms"))
        } finally {
            runCatching { process.destroy() }
        }
    }
    
    fun cleanup() {
        Shizuku.removeRequestPermissionResultListener(onRequestPermissionResultListener)
    }
    
    companion object {
        private const val SHIZUKU_PERMISSION_REQUEST_CODE = 1001
        private const val SHELL_TIMEOUT_MS = 10_000L
    }
}
