package com.example.inventoryapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.inventoryapp.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    /** Throws SQLiteConstraintException if the email already exists. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity)

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE UPPER(unique_code) = UPPER(:uniqueCode) LIMIT 1")
    suspend fun findByUniqueCode(uniqueCode: String): UserEntity?

    @Query("SELECT * FROM users WHERE phone_number = :phone OR phone_number = :cleanPhone LIMIT 1")
    suspend fun findByPhoneNumber(phone: String, cleanPhone: String = phone.replace(Regex("[^0-9+]"), "")): UserEntity?

    @Query("""
        SELECT * FROM users 
        WHERE (UPPER(unique_code) = UPPER(:query) 
           OR phone_number LIKE '%' || :query || '%')
          AND role = :expectedRole
        LIMIT 10
    """)
    suspend fun searchUsersByCodeOrPhone(query: String, expectedRole: com.example.inventoryapp.data.local.entity.UserRole): List<UserEntity>

    @Query("UPDATE users SET password_hash = :passwordHash, updated_at = :updatedAt WHERE id = :id")
    suspend fun updatePassword(id: String, passwordHash: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE users SET full_name = :fullName, phone_number = :phoneNumber, unique_code = :uniqueCode, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateProfile(id: String, fullName: String, phoneNumber: String, uniqueCode: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM users WHERE role = :role ORDER BY full_name ASC")
    fun observeUsersByRole(role: com.example.inventoryapp.data.local.entity.UserRole): Flow<List<UserEntity>>
}
