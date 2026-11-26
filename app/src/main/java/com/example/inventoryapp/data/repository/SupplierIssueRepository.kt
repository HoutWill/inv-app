package com.example.inventoryapp.data.repository

import android.util.Log
import com.example.inventoryapp.data.local.dao.SupplierIssueDao
import com.example.inventoryapp.data.local.entity.IssueSeverity
import com.example.inventoryapp.data.local.entity.SupplierIssueEntity
import com.example.inventoryapp.data.local.entity.SupplierIssueStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SupplierIssueRepository(
    private val issueDao: SupplierIssueDao
) {
    val allIssues: Flow<List<SupplierIssueEntity>> = issueDao.observeAllIssues()
    val openIssuesCount: Flow<Int> = issueDao.observeOpenIssuesCount()

    fun observeIssuesForSupplier(supplierId: String): Flow<List<SupplierIssueEntity>> =
        issueDao.observeIssuesForSupplier(supplierId)

    suspend fun reportIssue(
        supplierId: String,
        supplierName: String,
        middlemanId: String,
        middlemanName: String,
        middlemanCode: String,
        orderId: String? = null,
        orderNumber: String = "",
        issueType: String,
        severity: IssueSeverity,
        affectedItemName: String,
        affectedQuantity: Int,
        description: String,
        proofImageUri: String = ""
    ): Result<SupplierIssueEntity> = withContext(Dispatchers.IO) {
        try {
            if (description.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Please describe the issue details"))
            }
            val datePart = SimpleDateFormat("MMdd", Locale.US).format(Date())
            val suffix = (System.currentTimeMillis() % 10000).toString().padStart(4, '0')
            val issue = SupplierIssueEntity(
                issueCode = "ISS-$datePart-$suffix",
                orderId = orderId,
                orderNumber = orderNumber.trim(),
                supplierId = supplierId,
                supplierName = supplierName.trim(),
                middlemanId = middlemanId.ifBlank { "user_middleman" },
                middlemanName = middlemanName.trim().ifBlank { "Middle Man HQ" },
                middlemanCode = middlemanCode.trim().ifBlank { "MID-1001" },
                issueType = issueType.trim().ifBlank { "General Quality Issue" },
                severity = severity,
                affectedItemName = affectedItemName.trim(),
                affectedQuantity = affectedQuantity.coerceAtLeast(0),
                description = description.trim(),
                proofImageUri = proofImageUri.trim(),
                status = SupplierIssueStatus.OPEN,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            issueDao.insertIssue(issue)
            Result.success(issue)
        } catch (e: Exception) {
            Log.e("SupplierIssueRepo", "Error reporting supplier issue", e)
            Result.failure(e)
        }
    }

    suspend fun updateIssueStatus(
        issueId: String,
        newStatus: SupplierIssueStatus,
        supplierResponse: String
    ): Result<SupplierIssueEntity> = withContext(Dispatchers.IO) {
        try {
            val existing = issueDao.getIssueById(issueId)
                ?: return@withContext Result.failure(IllegalArgumentException("Issue report not found"))
            val now = System.currentTimeMillis()
            val updated = existing.copy(
                status = newStatus,
                supplierResponse = supplierResponse.trim().ifBlank { existing.supplierResponse },
                resolvedAt = if (newStatus == SupplierIssueStatus.RESOLVED || newStatus == SupplierIssueStatus.DISMISSED) now else existing.resolvedAt,
                updatedAt = now
            )
            issueDao.updateIssue(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Log.e("SupplierIssueRepo", "Error updating issue status", e)
            Result.failure(e)
        }
    }

    suspend fun deleteIssue(issueId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            issueDao.deleteIssue(issueId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
