package com.example.inventoryapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class ConnectionStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    DISCONNECTED
}

@Entity(
    tableName = "partner_connections",
    indices = [
        Index(value = ["requester_id"]),
        Index(value = ["receiver_id"]),
        Index(value = ["status"]),
        Index(value = ["requester_id", "receiver_id"])
    ]
)
data class PartnerConnectionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "requester_id") val requesterId: String,
    @ColumnInfo(name = "receiver_id") val receiverId: String,
    @ColumnInfo(name = "requester_role") val requesterRole: UserRole,
    @ColumnInfo(name = "receiver_role") val receiverRole: UserRole,
    val status: ConnectionStatus = ConnectionStatus.PENDING,
    @ColumnInfo(name = "request_note") val requestNote: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

data class PartnerConnectionWithUser(
    val connection: PartnerConnectionEntity,
    val partnerUser: UserEntity
)
