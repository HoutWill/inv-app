package com.example.inventoryapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "purchase_order_items",
    indices = [Index(value = ["orderId"])]
)
data class PurchaseOrderItemEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val orderId: String,
    val productId: String? = null,
    val sku: String,
    val productName: String,
    val quantity: Int,
    val unitCost: Double
)
