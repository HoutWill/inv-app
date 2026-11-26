package com.example.inventoryapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.inventoryapp.data.local.entity.ConnectionStatus
import com.example.inventoryapp.data.local.entity.PartnerConnectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PartnerConnectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(connection: PartnerConnectionEntity)

    @Update
    suspend fun update(connection: PartnerConnectionEntity)

    @Query("SELECT * FROM partner_connections WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): PartnerConnectionEntity?

    @Query("""
        SELECT * FROM partner_connections 
        WHERE (requester_id = :userId1 AND receiver_id = :userId2) 
           OR (requester_id = :userId2 AND receiver_id = :userId1)
        LIMIT 1
    """)
    suspend fun findExistingConnection(userId1: String, userId2: String): PartnerConnectionEntity?

    @Query("""
        SELECT * FROM partner_connections 
        WHERE receiver_id = :userId AND status = 'PENDING' 
        ORDER BY created_at DESC
    """)
    fun observeIncomingRequests(userId: String): Flow<List<PartnerConnectionEntity>>

    @Query("""
        SELECT * FROM partner_connections 
        WHERE requester_id = :userId AND status = 'PENDING' 
        ORDER BY created_at DESC
    """)
    fun observeSentRequests(userId: String): Flow<List<PartnerConnectionEntity>>

    @Query("""
        SELECT * FROM partner_connections 
        WHERE (requester_id = :userId OR receiver_id = :userId) AND status = 'ACCEPTED' 
        ORDER BY updated_at DESC
    """)
    fun observeActiveConnections(userId: String): Flow<List<PartnerConnectionEntity>>

    @Query("""
        SELECT COUNT(*) FROM partner_connections 
        WHERE receiver_id = :userId AND status = 'PENDING'
    """)
    fun observePendingIncomingCount(userId: String): Flow<Int>

    @Query("UPDATE partner_connections SET status = :status, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: ConnectionStatus, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM partner_connections WHERE id = :id")
    suspend fun deleteById(id: String)
}
