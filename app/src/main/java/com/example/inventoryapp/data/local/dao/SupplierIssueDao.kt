package com.example.inventoryapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.inventoryapp.data.local.entity.SupplierIssueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierIssueDao {
    @Query(
        """
        SELECT * FROM supplier_issues
        ORDER BY CASE status
            WHEN 'OPEN' THEN 0
            WHEN 'ACKNOWLEDGED' THEN 1
            WHEN 'REPLACEMENT_SENT' THEN 2
            WHEN 'RESOLVED' THEN 3
            ELSE 4
        END, created_at DESC
        """
    )
    fun observeAllIssues(): Flow<List<SupplierIssueEntity>>

    @Query("SELECT * FROM supplier_issues WHERE supplier_id = :supplierId ORDER BY created_at DESC")
    fun observeIssuesForSupplier(supplierId: String): Flow<List<SupplierIssueEntity>>

    @Query("SELECT COUNT(*) FROM supplier_issues WHERE status IN ('OPEN', 'ACKNOWLEDGED', 'REPLACEMENT_SENT')")
    fun observeOpenIssuesCount(): Flow<Int>

    @Query("SELECT * FROM supplier_issues WHERE id = :id LIMIT 1")
    suspend fun getIssueById(id: String): SupplierIssueEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIssue(issue: SupplierIssueEntity)

    @Update
    suspend fun updateIssue(issue: SupplierIssueEntity)

    @Query("DELETE FROM supplier_issues WHERE id = :id")
    suspend fun deleteIssue(id: String)
}
