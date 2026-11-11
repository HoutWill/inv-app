package com.example.inventoryapp.data.local.dao

import androidx.room.*
import com.example.inventoryapp.data.local.entity.PurchaseOrderEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderItemEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseOrderDao {
    @Query("SELECT * FROM purchase_orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<PurchaseOrderEntity>>

    @Query("SELECT * FROM purchase_orders WHERE supplierId = :supplierId ORDER BY createdAt DESC")
    fun getOrdersBySupplier(supplierId: String): Flow<List<PurchaseOrderEntity>>

    @Query("SELECT * FROM purchase_orders WHERE status = :status ORDER BY createdAt DESC")
    fun getOrdersByStatus(status: PurchaseOrderStatus): Flow<List<PurchaseOrderEntity>>

    @Query("SELECT * FROM purchase_orders WHERE status IN (:statuses) ORDER BY createdAt DESC")
    fun getOrdersByStatuses(statuses: List<PurchaseOrderStatus>): Flow<List<PurchaseOrderEntity>>

    @Query("SELECT * FROM purchase_orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: String): PurchaseOrderEntity?

    @Query("SELECT * FROM purchase_orders WHERE id = :id LIMIT 1")
    fun observeOrderById(id: String): Flow<PurchaseOrderEntity?>

    @Query("SELECT * FROM purchase_order_items WHERE orderId = :orderId")
    fun getItemsForOrder(orderId: String): Flow<List<PurchaseOrderItemEntity>>

    @Query("SELECT * FROM purchase_order_items WHERE orderId = :orderId")
    suspend fun getItemsForOrderSync(orderId: String): List<PurchaseOrderItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: PurchaseOrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<PurchaseOrderItemEntity>)

    @Update
    suspend fun updateOrder(order: PurchaseOrderEntity)

    @Query("UPDATE purchase_orders SET status = :newStatus, updatedAt = :updatedAt WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: String, newStatus: PurchaseOrderStatus, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM purchase_orders WHERE id = :id")
    suspend fun deleteOrder(id: String)

    @Query("DELETE FROM purchase_orders")
    suspend fun clearAllOrders()

    @Query("DELETE FROM purchase_order_items")
    suspend fun clearAllOrderItems()
}
