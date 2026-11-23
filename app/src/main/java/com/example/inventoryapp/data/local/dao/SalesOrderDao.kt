package com.example.inventoryapp.data.local.dao

import androidx.room.*
import com.example.inventoryapp.data.local.entity.SalesOrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SalesOrderDao {
    @Query("SELECT * FROM sales_orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<SalesOrderEntity>>

    @Query("SELECT * FROM sales_orders WHERE paymentStatus != 'PAID' ORDER BY createdAt DESC")
    fun getUnpaidOrders(): Flow<List<SalesOrderEntity>>

    @Query("SELECT COUNT(*) FROM sales_orders WHERE paymentStatus != 'PAID'")
    fun getUnpaidOrdersCount(): Flow<Int>

    @Query("SELECT * FROM sales_orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: String): SalesOrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: SalesOrderEntity)

    @Update
    suspend fun updateOrder(order: SalesOrderEntity)

    @Delete
    suspend fun deleteOrder(order: SalesOrderEntity)

    @Query("DELETE FROM sales_orders")
    suspend fun clearAllOrders()
}
