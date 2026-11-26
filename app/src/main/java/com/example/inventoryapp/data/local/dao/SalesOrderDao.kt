package com.example.inventoryapp.data.local.dao

import androidx.room.*
import com.example.inventoryapp.data.local.entity.SalesOrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SalesOrderDao {
    @Query("SELECT * FROM sales_orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<SalesOrderEntity>>

    @Query("SELECT * FROM sales_orders WHERE paymentStatus IN ('UNPAID', 'PARTIAL') ORDER BY createdAt DESC")
    fun getUnpaidOrders(): Flow<List<SalesOrderEntity>>

    @Query("SELECT COUNT(*) FROM sales_orders WHERE paymentStatus IN ('UNPAID', 'PARTIAL')")
    fun getUnpaidOrdersCount(): Flow<Int>

    @Query("SELECT * FROM sales_orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: String): SalesOrderEntity?

    @Query("UPDATE sales_orders SET paymentStatus = 'CANCELLED', cancelReason = :reason, cancelledAt = :cancelledAt WHERE id = :orderId")
    suspend fun voidOrder(orderId: String, reason: String, cancelledAt: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: SalesOrderEntity)

    @Update
    suspend fun updateOrder(order: SalesOrderEntity)

    @Delete
    suspend fun deleteOrder(order: SalesOrderEntity)

    @Query("DELETE FROM sales_orders")
    suspend fun clearAllOrders()
}
