package com.example.inventoryapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class PurchaseOrderStatus {
    PENDING,
    CONFIRMED,
    REJECTED,
    DELIVERED,  // "In Delivery" status shown on Behance mockup
    RECEIVED,   // Physical receipt completed by Owner -> inventory incremented
    CANCELLED
}

enum class SupplierPaymentMethod(val label: String, val subtitle: String) {
    COD("Cash on Delivery (COD)", "Pay supplier upon delivery / stock-in"),
    ABA_KHQR("ABA KHQR / Bank", "Transfer digitally via KHQR or bank"),
    CASH("Cash Upfront", "Pay supplier cash when placing order"),
    NET_TERMS("Net Terms", "Pay supplier on agreed credit schedule")
}

enum class SupplierPaymentStatus(val label: String) {
    UNPAID("Unpaid"),
    PAID("Paid")
}

@Entity(tableName = "purchase_orders")
data class PurchaseOrderEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val orderNumber: String,
    val supplierId: String,
    val supplierName: String,
    val status: PurchaseOrderStatus = PurchaseOrderStatus.PENDING,
    val totalAmount: Double = 0.0,
    val totalQuantity: Int = 0,
    val title: String = "",
    val movementType: String = "Goods Inbound",
    val picSender: String = "",
    val trackingNumber: String = "",
    val ownerNote: String = "",
    val supplierNote: String = "",
    val estArrival: String = "",
    val totalWeight: String = "",
    val paymentMethod: SupplierPaymentMethod = SupplierPaymentMethod.COD,
    val paymentStatus: SupplierPaymentStatus = SupplierPaymentStatus.UNPAID,
    val paidAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val confirmedAt: Long? = null,
    val deliveredAt: Long? = null,
    val receivedAt: Long? = null,
    val cancelledAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
