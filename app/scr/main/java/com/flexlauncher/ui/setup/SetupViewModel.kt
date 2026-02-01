package com.flexlauncher.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.flexlauncher.data.dao.SettingsDao
import com.flexlauncher.data.model.LauncherSettings
import com.flexlauncher.data.repository.AppRepository
import com.flexlauncher.data.repository.UserRepository
import com.flexlauncher.shizuku.ShizukuManager
import kotlinx.coroutines.launch

class SetupViewModel(
    private val settingsDao: SettingsDao,
    private val userRepository: UserRepository,
    private val appRepository: AppRepository,
    private val shizukuManager: ShizukuManager
) : ViewModel() {
    
    fun hasShizukuPermission(): Boolean {
        return shizukuManager.hasPermission()
    }
    
    fun requestShizukuPermission(callback: (Boolean) -> Unit) {
        shizukuManager.requestPermission(callback)
    }
    
    suspend fun addUser(name: String, password: String, photoPath: String?, isDefault: Boolean): Long {
        return userRepository.createUser(name, password, photoPath, isDefault)
    }
    
    suspend fun createDefaultSetup(isMultiUserMode: Boolean) {
        // Create default user if not multi-user mode
        if (!isMultiUserMode) {
            val userId = userRepository.createUser(
                name = "Default",
                password = "",
                photoPath = null,
                isDefault = true
            )
            
            // Load apps for default user
            appRepository.syncApps(userId)
        }
    }
    
    suspend fun completeSetup(isMultiUserMode: Boolean) {
        val settings = LauncherSettings(
            isMultiUserMode = isMultiUserMode,
            currentUserId = if (isMultiUserMode) 0L else {
                userRepository.getDefaultUser()?.id ?: 0L
            }
        )
        settingsDao.insertSettings(settings)
    }
}

class SetupViewModelFactory(
    private val settingsDao: SettingsDao,
    private val userRepository: UserRepository,
    private val appRepository: AppRepository,
    private val shizukuManager: ShizukuManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SetupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SetupViewModel(settingsDao, userRepository, appRepository, shizukuManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
