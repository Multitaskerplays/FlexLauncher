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
        refreshCurrentUser()
    }
    
    /** Re-reads the selected profile; call after settings change (e.g. unlock). */
    fun refreshCurrentUser() {
        viewModelScope.launch {
            val currentSettings = settingsDao.getSettingsOnce()
            if (currentSettings?.isMultiUserMode == true && currentSettings.currentUserId == 0L) {
                // Multi-user mode with nothing selected: stay locked. Falling back to a
                // "default" profile here would walk straight past the profile picker.
                _currentUser.value = null
                return@launch
            }
            val user = if (currentSettings != null && currentSettings.currentUserId != 0L) {
                userRepository.getUserById(currentSettings.currentUserId)
            } else {
                userRepository.getDefaultUser()
            }
            _currentUser.value = user ?: createFallbackUser()
        }
    }

    /**
     * Home screen with no profile means no app rows, i.e. a blank launcher. Rather than show
     * an empty grid, fall back to a default profile so the app list has somewhere to live.
     */
    private suspend fun createFallbackUser(): com.flexlauncher.data.model.User {
        val id = userRepository.createUser(
            name = "Default",
            password = "",
            photoPath = null,
            isDefault = true
        )
        return userRepository.getUserById(id)
            ?: throw IllegalStateException("could not create the default profile")
    }
    
    private var observedUserId: Long? = null
    private var appsLiveData: LiveData<List<AppInfo>>? = null
    private var appsObserver: Observer<List<AppInfo>>? = null
    private var syncing = false
    private var prunedForUser = false

    fun loadAppsForUser(userId: Long) {
        if (observedUserId == userId) return
        observedUserId = userId

        // observeForever without removing the previous observer leaks one per profile.
        appsObserver?.let { previous -> appsLiveData?.removeObserver(previous) }

        val live = appRepository.getAppsForUser(userId)
        val observer = Observer<List<AppInfo>> { apps ->
            _apps.value = apps
            // Self-heal when a package was removed while the launcher was not running: the
            // PACKAGE_REMOVED broadcast is not always delivered, but a stale row would keep
            // showing an app that no longer exists.
            if (!prunedForUser) {
                prunedForUser = true
                viewModelScope.launch {
                    runCatching { appRepository.pruneMissingApps(userId) }
                }
            }
            // A fresh profile (created after setup) has no rows yet, which would leave the
            // home screen empty. Populate it from the package manager the first time.
            if (apps.isEmpty() && !syncing) {
                syncing = true
                viewModelScope.launch {
                    try {
                        appRepository.syncApps(userId)
                    } finally {
                        syncing = false
                    }
                }
            }
        }
        appsLiveData = live
        appsObserver = observer
        live.observeForever(observer)
    }

    override fun onCleared() {
        appsObserver?.let { appsLiveData?.removeObserver(it) }
        super.onCleared()
    }
    
    fun launchApp(packageName: String) {
        // Launch even when no profile is known yet: returning silently here is what made
        // taps on the home grid look dead.
        appRepository.launchApp(packageName, _currentUser.value?.id ?: 0L)
    }

    fun hideApp(packageName: String) {
        val userId = _currentUser.value?.id ?: 0L
        viewModelScope.launch { appRepository.toggleHidden(packageName, userId) }
    }

    fun uninstallApp(packageName: String) {
        appRepository.uninstallApp(packageName)
    }

    fun recordLaunch(packageName: String) {
        val userId = _currentUser.value?.id ?: 0L
        viewModelScope.launch { appRepository.updateAppUsage(packageName, userId) }
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
