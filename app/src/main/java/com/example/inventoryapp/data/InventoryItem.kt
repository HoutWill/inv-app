package com.example.inventoryapp.data

data class InventoryItem(
    val id: String,
    val sku: String,
    val name: String,
    val category: String,
    val quantity: Int,
    val price: Double,
    val location: String
)
