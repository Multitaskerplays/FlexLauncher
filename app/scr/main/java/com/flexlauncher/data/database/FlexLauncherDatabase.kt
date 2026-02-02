package com.flexlauncher.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.flexlauncher.data.dao.*
import com.flexlauncher.data.model.*

@Database(
    entities = [
        User::class,
        AppInfo::class,
        AppCategory::class,
        LauncherSettings::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FlexLauncherDatabase : RoomDatabase() {
    
    abstract fun userDao(): UserDao
    abstract fun appDao(): AppDao
    abstract fun categoryDao(): CategoryDao
    abstract fun settingsDao(): SettingsDao
    
    companion object {
        @Volatile
        private var INSTANCE: FlexLauncherDatabase? = null
        
        fun getDatabase(context: Context): FlexLauncherDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FlexLauncherDatabase::class.java,
                    "flex_launcher_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
