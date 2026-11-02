package com.flexlauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class AppCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val icon: String,
    val color: Int,
    val position: Int = 0,
    val userId: Long = 0,
    val isDefault: Boolean = false
)

object DefaultCategories {
    val SOCIAL = "Social"
    val PRODUCTIVITY = "Productivity"
    val ENTERTAINMENT = "Entertainment"
    val UTILITIES = "Utilities"
    val GAMES = "Games"
    val COMMUNICATION = "Communication"
    val PHOTOGRAPHY = "Photography"
    val SHOPPING = "Shopping"
    val EDUCATION = "Education"
    val HEALTH = "Health & Fitness"
    val FINANCE = "Finance"
    val TRAVEL = "Travel"
    val NEWS = "News"
    val MUSIC = "Music"
    val VIDEO = "Video"
    val TOOLS = "Tools"
    val SYSTEM = "System"
    val OTHER = "Other"
    
    fun getDefaultList(): List<String> = listOf(
        SOCIAL, PRODUCTIVITY, ENTERTAINMENT, UTILITIES, GAMES,
        COMMUNICATION, PHOTOGRAPHY, SHOPPING, EDUCATION, HEALTH,
        FINANCE, TRAVEL, NEWS, MUSIC, VIDEO, TOOLS, SYSTEM, OTHER
    )
}
