package com.example.inventoryapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sku: String,
    val name: String,
    val category: String,
    val subcategory: String = "",
    val brand: String = "",
    val costPrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val quantity: Int = 0,
    val unit: String = "Pcs",
    val reorderLevel: Int = 5,
    val location: String = "Warehouse A",
    val imageUri: String? = null,
    val expirationDate: Long? = null,
    val supplierId: String? = null,
    val reorderQty: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

val ProductEntity.isDirectSlot: Boolean
    get() {
        val s = supplierId?.trim() ?: return true
        return s.isEmpty() ||
                s.equals("direct", ignoreCase = true) ||
                s.equals("none", ignoreCase = true) ||
                s.equals("direct_slot", ignoreCase = true) ||
                s.equals("own_direct_slot", ignoreCase = true)
    }

val ProductEntity.canRequestSupply: Boolean
    get() = quantity in 1..4 && !isDirectSlot

