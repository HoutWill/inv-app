package com.example.inventoryapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "stock_transactions")
data class StockTransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val sku: String,
    val productName: String,
    val transactionType: String,
    val quantityDelta: Int,
    val previousQuantity: Int,
    val newQuantity: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String? = null,
    val orderId: String? = null,
    val unitCost: Double? = null
)
