package com.flexlauncher.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.flexlauncher.data.model.User
import com.flexlauncher.data.model.AppInfo
import com.flexlauncher.data.model.LauncherSettings

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): LiveData<List<User>>
    
    @Query("SELECT * FROM users ORDER BY name ASC")
    suspend fun getAllUsersOnce(): List<User>

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: Long): User?
    
    @Query("SELECT * FROM users WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultUser(): User?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long
    
    @Update
    suspend fun updateUser(user: User)
    
    @Delete
    suspend fun deleteUser(user: User)
    
    @Query("UPDATE users SET lastAccessedAt = :time WHERE id = :userId")
    suspend fun updateLastAccessed(userId: Long, time: Long)
}

@Dao
interface AppDao {
    @Query("SELECT * FROM apps WHERE userId = :userId AND isHidden = 0 ORDER BY position ASC")
    fun getAppsForUser(userId: Long): LiveData<List<AppInfo>>
    
    @Query("SELECT * FROM apps WHERE userId = :userId")
    suspend fun getAppsForUserOnce(userId: Long): List<AppInfo>

    @Query("SELECT * FROM apps WHERE packageName = :packageName AND userId = :userId")
    suspend fun getApp(packageName: String, userId: Long): AppInfo?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppInfo)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<AppInfo>)
    
    @Update
    suspend fun updateApp(app: AppInfo)
    
    @Delete
    suspend fun deleteApp(app: AppInfo)
    
    @Query("UPDATE apps SET lastUsed = :time, launchCount = launchCount + 1 WHERE packageName = :packageName AND userId = :userId")
    suspend fun updateAppUsage(packageName: String, userId: Long, time: Long)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM launcher_settings WHERE id = 1")
    fun getSettings(): LiveData<LauncherSettings>
    
    @Query("SELECT * FROM launcher_settings WHERE id = 1")
    suspend fun getSettingsOnce(): LauncherSettings?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: LauncherSettings)
    
    @Update
    suspend fun updateSettings(settings: LauncherSettings)
}
