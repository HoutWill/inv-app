package com.example.inventoryapp.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.example.inventoryapp.data.local.AppDatabase
import com.example.inventoryapp.data.local.dao.ProductDao
import com.example.inventoryapp.data.local.dao.PurchaseOrderDao
import com.example.inventoryapp.data.local.dao.StockTransactionDao
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderItemEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderStatus
import com.example.inventoryapp.data.local.entity.StockTransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class OrderWithItems(
    val order: PurchaseOrderEntity,
    val items: List<PurchaseOrderItemEntity>
)

class OrderRepository(
    private val database: AppDatabase?,
    private val orderDao: PurchaseOrderDao,
    private val productDao: ProductDao,
    private val transactionDao: StockTransactionDao
) {
    private suspend fun <R> inTransaction(block: suspend () -> R): R {
        return if (database != null) {
            database.withTransaction { block() }
        } else {
            block()
        }
    }
    val allOrders: Flow<List<PurchaseOrderEntity>> = orderDao.getAllOrders()

    fun getOrdersForSupplier(supplierId: String): Flow<List<PurchaseOrderEntity>> =
        orderDao.getOrdersBySupplier(supplierId)

    fun getOrdersByStatuses(statuses: List<PurchaseOrderStatus>): Flow<List<PurchaseOrderEntity>> =
        orderDao.getOrdersByStatuses(statuses)

    fun observeOrder(id: String): Flow<PurchaseOrderEntity?> =
        orderDao.observeOrderById(id)

    fun observeOrderItems(orderId: String): Flow<List<PurchaseOrderItemEntity>> =
        orderDao.getItemsForOrder(orderId)

    suspend fun getOrderById(id: String): PurchaseOrderEntity? = withContext(Dispatchers.IO) {
        orderDao.getOrderById(id)
    }

    suspend fun getOrderWithItems(id: String): OrderWithItems? = withContext(Dispatchers.IO) {
        val order = orderDao.getOrderById(id) ?: return@withContext null
        val items = orderDao.getItemsForOrderSync(id)
        OrderWithItems(order, items)
    }

    suspend fun createOrder(
        supplierId: String,
        supplierName: String,
        title: String,
        picSender: String,
        items: List<PurchaseOrderItemEntity>,
        ownerNote: String = "",
        estArrival: String = ""
    ): Result<PurchaseOrderEntity> = withContext(Dispatchers.IO) {
        try {
            val count = System.currentTimeMillis() % 10000
            val dateStr = SimpleDateFormat("MMdd", Locale.getDefault()).format(Date())
            val orderNumber = "PO-$dateStr-${String.format(Locale.US, "%04d", count)}"
            val totalAmount = items.sumOf { it.quantity * it.unitCost }
            val totalQuantity = items.sumOf { it.quantity }

            val orderId = UUID.randomUUID().toString()
            val order = PurchaseOrderEntity(
                id = orderId,
                orderNumber = orderNumber,
                supplierId = supplierId,
                supplierName = supplierName,
                status = PurchaseOrderStatus.PENDING,
                totalAmount = totalAmount,
                totalQuantity = totalQuantity,
                title = title.ifBlank { "$orderNumber - Items from $supplierName" },
                movementType = "Goods Inbound",
                picSender = picSender,
                trackingNumber = "TRK-${UUID.randomUUID().toString().take(8).uppercase()}",
                ownerNote = ownerNote,
                estArrival = estArrival.ifBlank { "Next Business Day" },
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            val itemsWithOrderId = items.map { it.copy(orderId = orderId) }

            inTransaction {
                orderDao.insertOrder(order)
                orderDao.insertOrderItems(itemsWithOrderId)
            }

            Result.success(order)
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error creating order", e)
            Result.failure(e)
        }
    }

    suspend fun confirmOrder(orderId: String, note: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val order = orderDao.getOrderById(orderId)
                ?: return@withContext Result.failure(IllegalArgumentException("Order not found"))

            if (order.status != PurchaseOrderStatus.PENDING) {
                return@withContext Result.failure(IllegalStateException("Only PENDING orders can be confirmed"))
            }

            val updated = order.copy(
                status = PurchaseOrderStatus.CONFIRMED,
                supplierNote = note ?: order.supplierNote,
                confirmedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            orderDao.updateOrder(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error confirming order", e)
            Result.failure(e)
        }
    }

    suspend fun rejectOrder(orderId: String, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val order = orderDao.getOrderById(orderId)
                ?: return@withContext Result.failure(IllegalArgumentException("Order not found"))

            if (order.status != PurchaseOrderStatus.PENDING) {
                return@withContext Result.failure(IllegalStateException("Only PENDING orders can be rejected"))
            }

            val updated = order.copy(
                status = PurchaseOrderStatus.REJECTED,
                supplierNote = reason,
                updatedAt = System.currentTimeMillis()
            )
            orderDao.updateOrder(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error rejecting order", e)
            Result.failure(e)
        }
    }

    suspend fun markDelivered(orderId: String, trackingNumber: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val order = orderDao.getOrderById(orderId)
                ?: return@withContext Result.failure(IllegalArgumentException("Order not found"))

            if (order.status != PurchaseOrderStatus.CONFIRMED && order.status != PurchaseOrderStatus.PENDING) {
                return@withContext Result.failure(IllegalStateException("Order cannot be marked delivered in status ${order.status}"))
            }

            val updated = order.copy(
                status = PurchaseOrderStatus.DELIVERED,
                trackingNumber = trackingNumber ?: order.trackingNumber,
                deliveredAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            orderDao.updateOrder(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error marking order delivered", e)
            Result.failure(e)
        }
    }

    /**
     * Executes physical inbound receipt:
     * - Increments stock on hand for each item
     * - Recalculates weighted average cost
     * - Inserts PO_RECEIVED transaction audit logs
     * - Marks the order as RECEIVED
     * ALL in a single atomic database transaction.
     */
    suspend fun receiveOrder(orderId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            inTransaction {
                val order = orderDao.getOrderById(orderId)
                    ?: throw IllegalArgumentException("Order not found")

                if (order.status == PurchaseOrderStatus.RECEIVED) {
                    throw IllegalStateException("Order is already received")
                }
                if (order.status == PurchaseOrderStatus.CANCELLED || order.status == PurchaseOrderStatus.REJECTED) {
                    throw IllegalStateException("Cannot receive a cancelled or rejected order")
                }

                val items = orderDao.getItemsForOrderSync(orderId)

                for (item in items) {
                    val existingProduct = if (item.productId != null) {
                        productDao.getProductById(item.productId)
                    } else {
                        productDao.getProductBySku(item.sku)
                    }

                    if (existingProduct != null) {
                        val prevQty = existingProduct.quantity
                        val newQty = prevQty + item.quantity

                        // Weighted average cost calculation
                        val prevTotalCost = prevQty * existingProduct.costPrice
                        val addedTotalCost = item.quantity * item.unitCost
                        val newAverageCost = if (newQty > 0) (prevTotalCost + addedTotalCost) / newQty else item.unitCost

                        val updatedProduct = existingProduct.copy(
                            quantity = newQty,
                            costPrice = newAverageCost,
                            supplierId = existingProduct.supplierId ?: order.supplierId,
                            updatedAt = System.currentTimeMillis()
                        )
                        productDao.updateProduct(updatedProduct)

                        // Insert PO_RECEIVED transaction
                        transactionDao.insertTransaction(
                            StockTransactionEntity(
                                productId = updatedProduct.id,
                                sku = updatedProduct.sku,
                                productName = updatedProduct.name,
                                transactionType = "PO_RECEIVED",
                                quantityDelta = item.quantity,
                                previousQuantity = prevQty,
                                newQuantity = newQty,
                                note = "Received Inbound from ${order.supplierName} (${order.orderNumber})",
                                orderId = order.id,
                                unitCost = item.unitCost
                            )
                        )
                    } else {
                        // Create product into inventory if not yet registered
                        val newProduct = ProductEntity(
                            sku = item.sku,
                            name = item.productName,
                            category = "Grocery",
                            costPrice = item.unitCost,
                            sellingPrice = item.unitCost * 1.35, // standard 35% margin default
                            quantity = item.quantity,
                            unit = "Pcs",
                            supplierId = order.supplierId,
                            updatedAt = System.currentTimeMillis()
                        )
                        productDao.insertProduct(newProduct)

                        transactionDao.insertTransaction(
                            StockTransactionEntity(
                                productId = newProduct.id,
                                sku = newProduct.sku,
                                productName = newProduct.name,
                                transactionType = "PO_RECEIVED",
                                quantityDelta = item.quantity,
                                previousQuantity = 0,
                                newQuantity = item.quantity,
                                note = "Initial Inbound from ${order.supplierName} (${order.orderNumber})",
                                orderId = order.id,
                                unitCost = item.unitCost
                            )
                        )
                    }
                }

                // Update order status to RECEIVED
                val updatedOrder = order.copy(
                    status = PurchaseOrderStatus.RECEIVED,
                    receivedAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                orderDao.updateOrder(updatedOrder)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error receiving order into inventory", e)
            Result.failure(e)
        }
    }

    suspend fun cancelOrder(orderId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val order = orderDao.getOrderById(orderId)
                ?: return@withContext Result.failure(IllegalArgumentException("Order not found"))

            if (order.status != PurchaseOrderStatus.PENDING) {
                return@withContext Result.failure(IllegalStateException("Only PENDING orders can be cancelled"))
            }

            val updated = order.copy(
                status = PurchaseOrderStatus.CANCELLED,
                cancelledAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            orderDao.updateOrder(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error cancelling order", e)
            Result.failure(e)
        }
    }
}
