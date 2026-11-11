package com.example.inventoryapp

import com.example.inventoryapp.util.PasswordHasher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherUnitTest {

    @Test
    fun testHashAndVerifySuccess() {
        val password = "mySecretPassword123"
        val hash = PasswordHasher.hash(password, iterations = 1000) // Lower iteration count for fast unit test

        assertTrue("Verification should succeed with correct password", PasswordHasher.verify(password, hash))
    }

    @Test
    fun testHashAndVerifyWrongPassword() {
        val password = "mySecretPassword123"
        val hash = PasswordHasher.hash(password, iterations = 1000)

        assertFalse("Verification should fail with wrong password", PasswordHasher.verify("wrongPassword", hash))
    }

    @Test
    fun testMalformedHash() {
        assertFalse("Malformed hash should return false", PasswordHasher.verify("pass", "invalid_hash_string"))
        assertFalse("Malformed algorithm should return false", PasswordHasher.verify("pass", "md5\$1000\$salt\$hash"))
    }
}
