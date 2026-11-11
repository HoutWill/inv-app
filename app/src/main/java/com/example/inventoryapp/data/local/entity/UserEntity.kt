package com.example.inventoryapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** Who the account belongs to. Stored as its name ("OWNER" / "SUPPLIER"). */
enum class UserRole { OWNER, SUPPLIER }

/**
 * Local user account. Same columns will be used in Postgres (step 2).
 *
 * [passwordHash] is a self-describing PBKDF2 string: `pbkdf2_sha256$<iterations>$<saltHex>$<hashHex>`
 * (see PasswordHasher). The plain password is never stored.
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "full_name") val fullName: String,
    /** Always stored trimmed + lowercase. */
    val email: String,
    @ColumnInfo(name = "password_hash") val passwordHash: String,
    val role: UserRole = UserRole.OWNER,
    /** Only for SUPPLIER accounts; becomes a foreign key once the suppliers table exists. */
    @ColumnInfo(name = "supplier_id") val supplierId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
