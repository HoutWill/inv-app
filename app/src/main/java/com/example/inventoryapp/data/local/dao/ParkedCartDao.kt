package com.example.inventoryapp.data.local.dao

import androidx.room.*
import com.example.inventoryapp.data.local.entity.ParkedCartEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParkedCartDao {
    @Query("SELECT * FROM parked_carts ORDER BY createdAt DESC")
    fun getAllParkedCarts(): Flow<List<ParkedCartEntity>>

    @Query("SELECT COUNT(*) FROM parked_carts")
    fun getParkedCartsCount(): Flow<Int>

    @Query("SELECT * FROM parked_carts WHERE id = :id LIMIT 1")
    suspend fun getParkedCartById(id: String): ParkedCartEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParkedCart(cart: ParkedCartEntity)

    @Delete
    suspend fun deleteParkedCart(cart: ParkedCartEntity)

    @Query("DELETE FROM parked_carts WHERE id = :id")
    suspend fun deleteParkedCartById(id: String)
}
