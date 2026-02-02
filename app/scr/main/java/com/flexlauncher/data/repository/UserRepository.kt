package com.flexlauncher.data.repository

import androidx.lifecycle.LiveData
import com.flexlauncher.data.dao.UserDao
import com.flexlauncher.data.model.User
import java.security.MessageDigest

class UserRepository(private val userDao: UserDao) {
    
    val allUsers: LiveData<List<User>> = userDao.getAllUsers()
    
    suspend fun getUserById(userId: Long): User? {
        return userDao.getUserById(userId)
    }
    
    suspend fun getDefaultUser(): User? {
        return userDao.getDefaultUser()
    }
    
    suspend fun createUser(
        name: String,
        password: String,
        photoPath: String? = null,
        isDefault: Boolean = false
    ): Long {
        val passwordHash = hashPassword(password)
        val user = User(
            name = name,
            passwordHash = passwordHash,
            photoPath = photoPath,
            isDefault = isDefault
        )
        return userDao.insertUser(user)
    }
    
    suspend fun updateUser(user: User) {
        userDao.updateUser(user)
    }
    
    suspend fun deleteUser(user: User) {
        userDao.deleteUser(user)
    }
    
    suspend fun verifyPassword(userId: Long, password: String): Boolean {
        val user = getUserById(userId) ?: return false
        val inputHash = hashPassword(password)
        return user.passwordHash == inputHash
    }
    
    suspend fun updateLastAccessed(userId: Long) {
        userDao.updateLastAccessed(userId, System.currentTimeMillis())
    }
    
    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
