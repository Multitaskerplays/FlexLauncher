package com.flexlauncher.ui.main

import androidx.lifecycle.*
import com.flexlauncher.data.dao.SettingsDao
import com.flexlauncher.data.model.AppInfo
import com.flexlauncher.data.model.LauncherSettings
import com.flexlauncher.data.model.User
import com.flexlauncher.data.repository.AppRepository
import com.flexlauncher.data.repository.UserRepository
import kotlinx.coroutines.launch

class MainViewModel(
    private val settingsDao: SettingsDao,
    private val userRepository: UserRepository,
    private val appRepository: AppRepository
) : ViewModel() {
    
    val settings: LiveData<LauncherSettings> = settingsDao.getSettings()
    
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser
    
    private val _apps = MutableLiveData<List<AppInfo>>()
    val apps: LiveData<List<AppInfo>> = _apps
    
    init {
        loadCurrentUser()
    }
    
    private fun loadCurrentUser() {
        viewModelScope.launch {
            val currentSettings = settingsDao.getSettingsOnce()
            if (currentSettings != null && currentSettings.currentUserId != 0L) {
                val user = userRepository.getUserById(currentSettings.currentUserId)
                _currentUser.value = user
            } else {
                val defaultUser = userRepository.getDefaultUser()
                _currentUser.value = defaultUser
            }
        }
    }
    
    fun loadAppsForUser(userId: Long) {
        appRepository.getAppsForUser(userId).observeForever { apps ->
            _apps.value = apps
        }
    }
    
    fun launchApp(packageName: String) {
        val userId = _currentUser.value?.id ?: return
        appRepository.launchApp(packageName, userId)
    }
    
    fun handleSwipeUp() {
        // Open app drawer based on settings
        settings.value?.let {
            when (it.swipeUpAction) {
                "app_drawer" -> {
                    // TODO: Open app drawer
                }
                "search" -> {
                    // TODO: Open search
                }
                "recent_apps" -> {
                    // TODO: Show recent apps
                }
            }
        }
    }
    
    fun handleSwipeDown() {
        settings.value?.let {
            when (it.swipeDownAction) {
                "notifications" -> {
                    // TODO: Open notifications
                }
                "quick_settings" -> {
                    // TODO: Open quick settings
                }
            }
        }
    }
    
    fun handleSwipeLeft() {
        settings.value?.let {
            when (it.swipeLeftAction) {
                "widgets" -> {
                    // TODO: Show widgets
                }
            }
        }
    }
    
    fun handleSwipeRight() {
        settings.value?.let {
            when (it.swipeRightAction) {
                "widgets" -> {
                    // TODO: Show widgets
                }
            }
        }
    }
    
    fun handleDoubleTap() {
        settings.value?.let {
            if (it.doubleTabToSleep) {
                // TODO: Implement double tap to sleep
            }
        }
    }
}

class MainViewModelFactory(
    private val settingsDao: SettingsDao,
    private val userRepository: UserRepository,
    private val appRepository: AppRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(settingsDao, userRepository, appRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
