package com.example.inventoryapp.data.repository

import com.example.inventoryapp.data.local.dao.PartnerConnectionDao
import com.example.inventoryapp.data.local.dao.SupplierDao
import com.example.inventoryapp.data.local.dao.UserDao
import com.example.inventoryapp.data.local.entity.ConnectionStatus
import com.example.inventoryapp.data.local.entity.PartnerConnectionEntity
import com.example.inventoryapp.data.local.entity.PartnerConnectionWithUser
import com.example.inventoryapp.data.local.entity.SupplierEntity
import com.example.inventoryapp.data.local.entity.UserEntity
import com.example.inventoryapp.data.local.entity.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PartnerConnectionRepository(
    private val partnerConnectionDao: PartnerConnectionDao,
    private val userDao: UserDao,
    private val supplierDao: SupplierDao
) {

    /**
     * Search for a partner by Unique ID (e.g. SUP-2001 / MID-1001) or Phone Number.
     * Enforces strict cross-role matching:
     * - Middleman (OWNER) can only find Suppliers (SUPPLIER)
     * - Supplier (SUPPLIER) can only find Middlemen (OWNER)
     */
    suspend fun searchPartner(
        query: String,
        currentUser: UserEntity
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a Unique ID or Phone Number"))
        }

        val expectedRole = if (currentUser.role == UserRole.OWNER) UserRole.SUPPLIER else UserRole.OWNER
        val queryUpper = cleanQuery.uppercase()

        // 1. Try finding by unique code
        var candidate = userDao.findByUniqueCode(queryUpper)

        // 2. Try finding by phone number if not found
        if (candidate == null) {
            candidate = userDao.findByPhoneNumber(cleanQuery)
        }

        // 3. Fallback search (contains phone or matches code)
        if (candidate == null) {
            val matches = userDao.searchUsersByCodeOrPhone(cleanQuery, expectedRole)
            candidate = matches.firstOrNull()
        }

        if (candidate == null) {
            return@withContext Result.failure(
                NoSuchElementException("No partner found matching '$cleanQuery'. Please check the ID or Phone Number.")
            )
        }

        // Validate not self
        if (candidate.id == currentUser.id || candidate.email.equals(currentUser.email, ignoreCase = true)) {
            return@withContext Result.failure(
                IllegalArgumentException("You cannot connect with your own account.")
            )
        }

        // Validate expected role
        if (candidate.role != expectedRole) {
            val roleName = if (currentUser.role == UserRole.OWNER) "Suppliers" else "Middlemen"
            return@withContext Result.failure(
                IllegalArgumentException("You can only search for and connect with $roleName.")
            )
        }

        Result.success(candidate)
    }

    /**
     * Send a connection request from the current user to a target user.
     */
    suspend fun sendConnectionRequest(
        sender: UserEntity,
        target: UserEntity,
        note: String
    ): Result<PartnerConnectionEntity> = withContext(Dispatchers.IO) {
        try {
            if (sender.id == target.id) {
                return@withContext Result.failure(IllegalArgumentException("Cannot send a request to yourself"))
            }

            val existing = partnerConnectionDao.findExistingConnection(sender.id, target.id)
            if (existing != null) {
                when (existing.status) {
                    ConnectionStatus.ACCEPTED -> {
                        return@withContext Result.failure(
                            IllegalStateException("You are already connected with ${target.fullName}")
                        )
                    }
                    ConnectionStatus.PENDING -> {
                        val isSender = existing.requesterId == sender.id
                        val msg = if (isSender) {
                            "You already sent a connection request to this partner (Pending approval)"
                        } else {
                            "This partner already sent you a request! Check your incoming requests to accept."
                        }
                        return@withContext Result.failure(IllegalStateException(msg))
                    }
                    ConnectionStatus.REJECTED, ConnectionStatus.DISCONNECTED -> {
                        // Re-open request
                        val reopened = existing.copy(
                            requesterId = sender.id,
                            receiverId = target.id,
                            requesterRole = sender.role,
                            receiverRole = target.role,
                            status = ConnectionStatus.PENDING,
                            requestNote = note.trim(),
                            updatedAt = System.currentTimeMillis()
                        )
                        partnerConnectionDao.update(reopened)
                        return@withContext Result.success(reopened)
                    }
                }
            }

            val newConnection = PartnerConnectionEntity(
                requesterId = sender.id,
                receiverId = target.id,
                requesterRole = sender.role,
                receiverRole = target.role,
                status = ConnectionStatus.PENDING,
                requestNote = note.trim()
            )
            partnerConnectionDao.insert(newConnection)
            Result.success(newConnection)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observe pending incoming requests for a user, paired with the sender's profile.
     */
    fun observeIncomingRequests(userId: String): Flow<List<PartnerConnectionWithUser>> {
        return partnerConnectionDao.observeIncomingRequests(userId).map { list ->
            list.mapNotNull { conn ->
                val partner = userDao.findById(conn.requesterId)
                if (partner != null) PartnerConnectionWithUser(conn, partner) else null
            }
        }.flowOn(Dispatchers.IO)
    }

    /**
     * Observe pending sent requests, paired with the receiver's profile.
     */
    fun observeSentRequests(userId: String): Flow<List<PartnerConnectionWithUser>> {
        return partnerConnectionDao.observeSentRequests(userId).map { list ->
            list.mapNotNull { conn ->
                val partner = userDao.findById(conn.receiverId)
                if (partner != null) PartnerConnectionWithUser(conn, partner) else null
            }
        }.flowOn(Dispatchers.IO)
    }

    /**
     * Observe active accepted connections, paired with the other party's profile.
     */
    fun observeActiveConnections(userId: String): Flow<List<PartnerConnectionWithUser>> {
        return partnerConnectionDao.observeActiveConnections(userId).map { list ->
            list.mapNotNull { conn ->
                val partnerId = if (conn.requesterId == userId) conn.receiverId else conn.requesterId
                val partner = userDao.findById(partnerId)
                if (partner != null) PartnerConnectionWithUser(conn, partner) else null
            }
        }.flowOn(Dispatchers.IO)
    }

    /**
     * Real-time count of pending incoming requests (for notification badges).
     */
    fun observePendingIncomingCount(userId: String): Flow<Int> {
        return partnerConnectionDao.observePendingIncomingCount(userId)
    }

    /**
     * Accept a connection request and auto-link directories.
     */
    suspend fun acceptConnection(connectionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val conn = partnerConnectionDao.findById(connectionId)
                ?: return@withContext Result.failure(IllegalArgumentException("Connection request not found"))

            partnerConnectionDao.updateStatus(connectionId, ConnectionStatus.ACCEPTED)

            // Auto-link into Supplier directory if one is Supplier and other is Middleman
            val requester = userDao.findById(conn.requesterId)
            val receiver = userDao.findById(conn.receiverId)
            val supplierUser = when {
                requester?.role == UserRole.SUPPLIER -> requester
                receiver?.role == UserRole.SUPPLIER -> receiver
                else -> null
            }

            if (supplierUser != null) {
                val cleanName = supplierUser.fullName.replace(" (Supplier)", "").trim()
                val existingSupplier = supplierDao.getSupplierById(supplierUser.id)
                    ?: supplierDao.getSupplierByEmail(supplierUser.email)

                if (existingSupplier == null) {
                    supplierDao.insertSupplier(
                        SupplierEntity(
                            id = supplierUser.id,
                            name = cleanName.ifBlank { "Supplier Partner" },
                            contactName = supplierUser.fullName,
                            phone = supplierUser.phoneNumber,
                            email = supplierUser.email,
                            note = "Connected Partner (${supplierUser.uniqueCode})",
                            isActive = true
                        )
                    )
                } else if (!existingSupplier.isActive) {
                    supplierDao.updateSupplier(existingSupplier.copy(isActive = true))
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Decline a connection request.
     */
    suspend fun declineConnection(connectionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            partnerConnectionDao.updateStatus(connectionId, ConnectionStatus.REJECTED)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cancel a sent request.
     */
    suspend fun cancelRequest(connectionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            partnerConnectionDao.deleteById(connectionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Disconnect an active partner while preserving order history.
     */
    suspend fun disconnectPartner(connectionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val conn = partnerConnectionDao.findById(connectionId)
                ?: return@withContext Result.failure(IllegalArgumentException("Connection not found"))

            partnerConnectionDao.updateStatus(connectionId, ConnectionStatus.DISCONNECTED)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
