package com.example.inventoryapp.data.repository

import android.util.Log
import com.example.inventoryapp.data.local.dao.ProductDao
import com.example.inventoryapp.data.local.dao.StockTransactionDao
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.data.local.entity.StockTransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class InventoryRepository(
    private val productDao: ProductDao,
    private val transactionDao: StockTransactionDao
) {
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val allTransactions: Flow<List<StockTransactionEntity>> = transactionDao.getAllTransactions()
    val lowStockProducts: Flow<List<ProductEntity>> = productDao.getLowStockProducts()

    suspend fun clearAllData(): Boolean = withContext(Dispatchers.IO) {
        try {
            productDao.clearAllProducts()
            transactionDao.clearAllTransactions()
            true
        } catch (e: Exception) {
            Log.e("InventoryRepository", "Error clearing all data", e)
            false
        }
    }

    fun searchProducts(query: String): Flow<List<ProductEntity>> {
        return if (query.isBlank()) {
            productDao.getAllProducts()
        } else {
            productDao.searchProducts(query)
        }
    }

    suspend fun getProductBySku(sku: String): ProductEntity? = withContext(Dispatchers.IO) {
        try {
            productDao.getProductBySku(sku)
        } catch (e: Exception) {
            Log.e("InventoryRepository", "Error fetching product by SKU: $sku", e)
            null
        }
    }

    suspend fun addProduct(product: ProductEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            productDao.insertProduct(product)
            try {
                transactionDao.insertTransaction(
                    StockTransactionEntity(
                        productId = product.id,
                        sku = product.sku,
                        productName = product.name,
                        transactionType = "NEW_ITEM",
                        quantityDelta = product.quantity,
                        previousQuantity = 0,
                        newQuantity = product.quantity,
                        note = "New product registered into system"
                    )
                )
            } catch (te: Exception) {
                Log.w("InventoryRepository", "Could not record transaction log", te)
            }
            true
        } catch (e: Exception) {
            Log.e("InventoryRepository", "Error adding product to database", e)
            false
        }
    }

    suspend fun updateStock(
        product: ProductEntity,
        quantityDelta: Int,
        transactionType: String,
        note: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val newQuantity = (product.quantity + quantityDelta).coerceAtLeast(0)
            val updatedProduct = product.copy(
                quantity = newQuantity,
                updatedAt = System.currentTimeMillis()
            )
            productDao.updateProduct(updatedProduct)

            try {
                transactionDao.insertTransaction(
                    StockTransactionEntity(
                        productId = product.id,
                        sku = product.sku,
                        productName = product.name,
                        transactionType = transactionType,
                        quantityDelta = quantityDelta,
                        previousQuantity = product.quantity,
                        newQuantity = newQuantity,
                        note = note ?: "Stock adjusted"
                    )
                )
            } catch (te: Exception) {
                Log.w("InventoryRepository", "Could not record stock update transaction", te)
            }
            true
        } catch (e: Exception) {
            Log.e("InventoryRepository", "Error updating stock", e)
            false
        }
    }

    suspend fun updateProduct(product: ProductEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            productDao.updateProduct(product.copy(updatedAt = System.currentTimeMillis()))
            true
        } catch (e: Exception) {
            Log.e("InventoryRepository", "Error updating product", e)
            false
        }
    }

    suspend fun deleteProduct(product: ProductEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            productDao.deleteProduct(product)
            true
        } catch (e: Exception) {
            Log.e("InventoryRepository", "Error deleting product", e)
            false
        }
    }
}
