package com.flexlauncher

import android.app.Application
import com.flexlauncher.data.database.FlexLauncherDatabase
import com.flexlauncher.data.repository.AppRepository
import com.flexlauncher.data.repository.UserRepository
import com.flexlauncher.shizuku.ShizukuManager

class FlexLauncherApp : Application() {
    
    lateinit var database: FlexLauncherDatabase
        private set
    
    lateinit var userRepository: UserRepository
        private set
        
    lateinit var appRepository: AppRepository
        private set
        
    lateinit var shizukuManager: ShizukuManager
        private set
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize database
        database = FlexLauncherDatabase.getDatabase(this)
        
        // Initialize Shizuku
        shizukuManager = ShizukuManager(this)
        
        // Initialize repositories
        userRepository = UserRepository(database.userDao())
        appRepository = AppRepository(this, database.appDao(), shizukuManager)
    }
    
    companion object {
        lateinit var instance: FlexLauncherApp
            private set
    }
}
