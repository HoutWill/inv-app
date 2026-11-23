package com.example.inventoryapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class PosPaymentMethod {
    ABA_KHQR,
    CASH,
    CARD,
    PAY_LATER,
    COD,
    ALIPAY,
    WECHAT
}

enum class PosPaymentStatus {
    PAID,
    PARTIAL,
    UNPAID
}

@Entity(
    tableName = "sales_orders",
    indices = [
        Index(value = ["paymentStatus"]),
        Index(value = ["createdAt"])
    ]
)
data class SalesOrderEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val invoiceNumber: String,
    val customerName: String,
    val customerPhone: String = "",
    val totalAmountUsd: Double,
    val totalAmountKhr: Long,
    val paidAmountUsd: Double = 0.0,
    val remainingAmountUsd: Double = totalAmountUsd,
    val paymentStatus: PosPaymentStatus = PosPaymentStatus.PAID,
    val paymentMethod: PosPaymentMethod = PosPaymentMethod.CASH,
    val dueDate: Long? = null,
    val note: String = "",
    val itemsSummary: String = "",
    val itemCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val settledAt: Long? = null
)
