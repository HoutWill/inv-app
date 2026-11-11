package com.example.inventoryapp.data.repository

import com.example.inventoryapp.data.local.dao.UserDao
import com.example.inventoryapp.data.local.entity.UserEntity
import com.example.inventoryapp.data.local.entity.UserRole
import com.example.inventoryapp.data.prefs.AppSessionStore
import com.example.inventoryapp.util.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepository(
    private val userDao: UserDao,
    private val sessionStore: AppSessionStore
) {
    val currentUser: Flow<UserEntity?> = sessionStore.loggedInUserId.flatMapLatest { userId ->
        if (userId.isNullOrBlank()) {
            flowOf(null)
        } else {
            userDao.observeById(userId)
        }
    }

    suspend fun getCurrentUser(): UserEntity? = withContext(Dispatchers.IO) {
        val currentId = sessionStore.loggedInUserId.firstOrNull() ?: return@withContext null
        userDao.findById(currentId)
    }

    suspend fun signUp(
        fullName: String,
        email: String,
        password: String
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val cleanName = fullName.trim()
            val cleanEmail = email.trim().lowercase()

            if (cleanName.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Please enter your full name"))
            }
            if (!isValidEmail(cleanEmail)) {
                return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address"))
            }
            if (password.length < 6) {
                return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
            }

            val existing = userDao.findByEmail(cleanEmail)
            if (existing != null) {
                return@withContext Result.failure(IllegalArgumentException("An account with this email already exists"))
            }

            val passwordHash = PasswordHasher.hash(password)
            val newUser = UserEntity(
                fullName = cleanName,
                email = cleanEmail,
                passwordHash = passwordHash,
                role = UserRole.OWNER
            )
            userDao.insert(newUser)
            sessionStore.setLoggedInUserId(newUser.id)
            Result.success(newUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(
        email: String,
        password: String
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()
            if (!isValidEmail(cleanEmail)) {
                return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address"))
            }
            if (password.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Please enter your password"))
            }

            val user = userDao.findByEmail(cleanEmail)
                ?: return@withContext Result.failure(IllegalArgumentException("No account found with this email"))

            val passwordMatches = PasswordHasher.verify(password, user.passwordHash)
            if (!passwordMatches) {
                return@withContext Result.failure(IllegalArgumentException("Incorrect password"))
            }

            sessionStore.setLoggedInUserId(user.id)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(
        email: String,
        newPassword: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()
            if (!isValidEmail(cleanEmail)) {
                return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address"))
            }
            if (newPassword.length < 6) {
                return@withContext Result.failure(IllegalArgumentException("New password must be at least 6 characters"))
            }

            val user = userDao.findByEmail(cleanEmail)
                ?: return@withContext Result.failure(IllegalArgumentException("No account found with this email"))

            val newHash = PasswordHasher.hash(newPassword)
            userDao.updatePassword(user.id, newHash)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        sessionStore.clearSession()
    }

    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    private fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && emailRegex.matches(email)
    }
}
