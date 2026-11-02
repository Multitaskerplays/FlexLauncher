package com.flexlauncher.data.repository

import androidx.lifecycle.LiveData
import com.flexlauncher.data.dao.UserDao
import com.flexlauncher.data.model.User
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

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
        val passwordHash = if (password.isEmpty()) "" else hashPassword(password)
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
        val stored = user.passwordHash
        if (stored.isEmpty() || stored == LEGACY_EMPTY_PASSWORD_HASH) {
            return true // profiles without a password just unlock
        }
        return if (stored.startsWith(PBKDF2_PREFIX)) {
            val parts = stored.split('$')
            if (parts.size != 4) return false
            val salt = Base64.getDecoder().decode(parts[2])
            val expected = Base64.getDecoder().decode(parts[3])
            MessageDigest.isEqual(expected, pbkdf2(password, salt, parts[1].toIntOrNull() ?: ITERATIONS))
        } else {
            // Hashes written before 2.x were a bare SHA-256; keep them working.
            MessageDigest.isEqual(
                stored.toByteArray(),
                MessageDigest.getInstance("SHA-256").digest(password.toByteArray()).toHex().toByteArray()
            )
        }
    }
    
    suspend fun updateLastAccessed(userId: Long) {
        userDao.updateLastAccessed(userId, System.currentTimeMillis())
    }
    
    /**
     * `pbkdf2$iterations$salt$hash`, all base64. Salting inside the value means no schema
     * migration: legacy bare SHA-256 strings still verify through the fallback above.
     */
    private fun hashPassword(password: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2(password, salt, ITERATIONS)
        val encoder = Base64.getEncoder()
        return PBKDF2_PREFIX + '$' + ITERATIONS + '$' +
            encoder.encodeToString(salt) + '$' + encoder.encodeToString(hash)
    }

    private fun pbkdf2(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, HASH_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private companion object {
        const val PBKDF2_PREFIX = "pbkdf2"
        const val ITERATIONS = 120_000
        const val SALT_BYTES = 16
        const val HASH_BITS = 256

        /** Older builds hashed the empty string for password-less profiles. */
        const val LEGACY_EMPTY_PASSWORD_HASH =
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    }
}
