package com.example.inventoryapp.data.repository

import android.util.Log
import com.example.inventoryapp.data.local.dao.SupplierDao
import com.example.inventoryapp.data.local.entity.SupplierEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SupplierRepository(
    private val supplierDao: SupplierDao
) {
    val allSuppliers: Flow<List<SupplierEntity>> = supplierDao.getAllSuppliers()
    val activeSuppliers: Flow<List<SupplierEntity>> = supplierDao.getActiveSuppliers()

    suspend fun getSupplierById(id: String): SupplierEntity? = withContext(Dispatchers.IO) {
        try {
            supplierDao.getSupplierById(id)
        } catch (e: Exception) {
            Log.e("SupplierRepository", "Error getting supplier by id: $id", e)
            null
        }
    }

    suspend fun getSupplierByEmail(email: String): SupplierEntity? = withContext(Dispatchers.IO) {
        try {
            supplierDao.getSupplierByEmail(email)
        } catch (e: Exception) {
            Log.e("SupplierRepository", "Error getting supplier by email: $email", e)
            null
        }
    }

    suspend fun saveSupplier(supplier: SupplierEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supplierDao.insertSupplier(
                supplier.copy(updatedAt = System.currentTimeMillis())
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SupplierRepository", "Error saving supplier", e)
            Result.failure(e)
        }
    }

    suspend fun toggleActive(id: String, isActive: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = supplierDao.getSupplierById(id)
            if (existing != null) {
                supplierDao.updateSupplier(existing.copy(isActive = isActive, updatedAt = System.currentTimeMillis()))
                Result.success(Unit)
            } else {
                Result.failure(IllegalArgumentException("Supplier not found"))
            }
        } catch (e: Exception) {
            Log.e("SupplierRepository", "Error toggling active state", e)
            Result.failure(e)
        }
    }

    suspend fun deleteSupplier(supplier: SupplierEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supplierDao.deleteSupplier(supplier)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SupplierRepository", "Error deleting supplier", e)
            Result.failure(e)
        }
    }
}
