package com.flexlauncher.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.flexlauncher.data.model.User
import com.flexlauncher.data.model.AppInfo
import com.flexlauncher.data.model.AppCategory
import com.flexlauncher.data.model.LauncherSettings

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): LiveData<List<User>>
    
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
    
    @Query("SELECT * FROM apps WHERE userId = :userId AND categoryId = :categoryId AND isHidden = 0")
    fun getAppsByCategory(userId: Long, categoryId: Int): LiveData<List<AppInfo>>
    
    @Query("SELECT * FROM apps WHERE packageName = :packageName AND userId = :userId")
    suspend fun getApp(packageName: String, userId: Long): AppInfo?
    
    @Query("SELECT * FROM apps WHERE userId = :userId AND isFavorite = 1")
    fun getFavoriteApps(userId: Long): LiveData<List<AppInfo>>
    
    @Query("SELECT * FROM apps WHERE userId = :userId ORDER BY lastUsed DESC LIMIT :limit")
    fun getRecentApps(userId: Long, limit: Int): LiveData<List<AppInfo>>
    
    @Query("SELECT * FROM apps WHERE userId = :userId ORDER BY launchCount DESC LIMIT :limit")
    fun getMostUsedApps(userId: Long, limit: Int): LiveData<List<AppInfo>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppInfo)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<AppInfo>)
    
    @Update
    suspend fun updateApp(app: AppInfo)
    
    @Delete
    suspend fun deleteApp(app: AppInfo)
    
    @Query("DELETE FROM apps WHERE packageName = :packageName")
    suspend fun deleteAppByPackage(packageName: String)
    
    @Query("UPDATE apps SET lastUsed = :time, launchCount = launchCount + 1 WHERE packageName = :packageName AND userId = :userId")
    suspend fun updateAppUsage(packageName: String, userId: Long, time: Long)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY position ASC")
    fun getCategoriesForUser(userId: Long): LiveData<List<AppCategory>>
    
    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: Int): AppCategory?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: AppCategory): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<AppCategory>)
    
    @Update
    suspend fun updateCategory(category: AppCategory)
    
    @Delete
    suspend fun deleteCategory(category: AppCategory)
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
