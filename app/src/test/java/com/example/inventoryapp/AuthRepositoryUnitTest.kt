package com.example.inventoryapp

import com.example.inventoryapp.data.local.dao.UserDao
import com.example.inventoryapp.data.local.entity.UserEntity
import com.example.inventoryapp.data.local.entity.UserRole
import com.example.inventoryapp.data.prefs.AppSessionStore
import com.example.inventoryapp.data.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeUserDao : UserDao {
    val users = mutableMapOf<String, UserEntity>()

    override suspend fun insert(user: UserEntity) {
        if (users.values.any { it.email.equals(user.email, ignoreCase = true) }) {
            throw Exception("UNIQUE constraint failed: users.email")
        }
        users[user.id] = user
    }

    override suspend fun findByEmail(email: String): UserEntity? {
        return users.values.firstOrNull { it.email.equals(email, ignoreCase = true) }
    }

    override suspend fun findById(id: String): UserEntity? {
        return users[id]
    }

    override fun observeById(id: String): Flow<UserEntity?> {
        return MutableStateFlow(users[id])
    }

    override suspend fun updatePassword(id: String, passwordHash: String, updatedAt: Long) {
        val user = users[id] ?: return
        users[id] = user.copy(passwordHash = passwordHash, updatedAt = updatedAt)
    }
}

class FakeSessionStore : AppSessionStore {
    private val introFlow = MutableStateFlow(false)
    val loggedInUserIdFlow = MutableStateFlow<String?>(null)

    override val hasSeenIntro: Flow<Boolean> = introFlow
    override val loggedInUserId: Flow<String?> = loggedInUserIdFlow

    override suspend fun setHasSeenIntro(seen: Boolean) {
        introFlow.value = seen
    }

    override suspend fun setLoggedInUserId(userId: String?) {
        loggedInUserIdFlow.value = userId
    }

    override suspend fun clearSession() {
        loggedInUserIdFlow.value = null
    }
}

class AuthRepositoryUnitTest {

    private lateinit var fakeDao: FakeUserDao
    private lateinit var fakeSession: FakeSessionStore
    private lateinit var repository: AuthRepository

    @Before
    fun setup() {
        fakeDao = FakeUserDao()
        fakeSession = FakeSessionStore()
        repository = AuthRepository(fakeDao, fakeSession)
    }

    @Test
    fun testSignUpSuccess() = runBlocking {
        val result = repository.signUp("Alice Clara", "aliceclara@gmail.com", "secret123")
        assertTrue("Sign up should succeed", result.isSuccess)

        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals("Alice Clara", user?.fullName)
        assertEquals("aliceclara@gmail.com", user?.email)
        assertEquals(UserRole.OWNER, user?.role)

        assertEquals("Session should store new user ID", user?.id, fakeSession.loggedInUserId.first())
    }

    @Test
    fun testSignUpDuplicateEmailFails() = runBlocking {
        repository.signUp("Alice Clara", "aliceclara@gmail.com", "secret123")

        val duplicate = repository.signUp("Alice Two", "aliceclara@gmail.com", "newsecret123")
        assertTrue("Duplicate email registration should fail", duplicate.isFailure)
    }

    @Test
    fun testSignUpValidation() = runBlocking {
        val blankName = repository.signUp("", "valid@email.com", "secret123")
        assertTrue("Blank name should fail", blankName.isFailure)

        val invalidEmail = repository.signUp("Alice", "notanemail", "secret123")
        assertTrue("Invalid email should fail", invalidEmail.isFailure)

        val shortPassword = repository.signUp("Alice", "valid@email.com", "123")
        assertTrue("Short password should fail", shortPassword.isFailure)
    }

    @Test
    fun testLoginSuccess() = runBlocking {
        repository.signUp("Bob Manager", "bob@example.com", "passwordBob123")
        fakeSession.clearSession()

        val loginResult = repository.login("bob@example.com", "passwordBob123")
        assertTrue("Login should succeed", loginResult.isSuccess)
        assertEquals("Bob Manager", loginResult.getOrNull()?.fullName)
        assertEquals(loginResult.getOrNull()?.id, fakeSession.loggedInUserId.first())
    }

    @Test
    fun testLoginWrongPasswordFails() = runBlocking {
        repository.signUp("Bob Manager", "bob@example.com", "passwordBob123")

        val loginResult = repository.login("bob@example.com", "wrongPassword")
        assertTrue("Login with wrong password should fail", loginResult.isFailure)
    }

    @Test
    fun testLoginNonExistentEmailFails() = runBlocking {
        val loginResult = repository.login("unknown@example.com", "anyPassword")
        assertTrue("Login for non-existent account should fail", loginResult.isFailure)
    }

    @Test
    fun testResetPasswordSuccess() = runBlocking {
        repository.signUp("Charlie", "charlie@example.com", "initialPass123")

        val resetResult = repository.resetPassword("charlie@example.com", "newSecurePassword456")
        assertTrue("Reset password should succeed", resetResult.isSuccess)

        // Old password should fail now
        val oldLogin = repository.login("charlie@example.com", "initialPass123")
        assertTrue("Old password should no longer work", oldLogin.isFailure)

        // New password should succeed
        val newLogin = repository.login("charlie@example.com", "newSecurePassword456")
        assertTrue("New password should work", newLogin.isSuccess)
    }

    @Test
    fun testLogoutClearsSession() = runBlocking {
        repository.signUp("Dave", "dave@example.com", "passDave123")
        assertNotNull(fakeSession.loggedInUserId.first())

        repository.logout()
        assertNull("Session should be null after logout", fakeSession.loggedInUserId.first())
    }
}
