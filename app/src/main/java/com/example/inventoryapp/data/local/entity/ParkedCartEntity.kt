package com.example.inventoryapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "parked_carts")
data class ParkedCartEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val note: String = "",
    val itemsJson: String, // serialized JSON array of items
    val totalAmountUsd: Double,
    val itemCount: Int,
    val createdAt: Long = System.currentTimeMillis()
)
