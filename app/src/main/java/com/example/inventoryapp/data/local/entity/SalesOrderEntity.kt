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
    WECHAT,
    SPLIT
}

enum class PosPaymentStatus {
    PAID,
    PARTIAL,
    UNPAID,
    CANCELLED,
    PARTIALLY_REFUNDED,
    REFUNDED
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
    val itemsJson: String = "",
    val itemCount: Int = 0,
    val subtotalUsd: Double = totalAmountUsd,
    val discountType: String = "NONE", // "NONE", "PERCENT", "FIXED"
    val discountValue: Double = 0.0,
    val discountAmountUsd: Double = 0.0,
    val taxRate: Double = 0.0, // e.g. 0.10 for 10% VAT
    val taxAmountUsd: Double = 0.0,
    val splitDetails: String = "",
    val cancelReason: String = "",
    val cancelledAt: Long? = null,
    val refundedAmountUsd: Double = 0.0,
    val refundIssueType: String = "",
    val refundConditionAnswer: String = "",
    val refundReason: String = "",
    val refundRestockAction: String = "RESTOCK",
    val refundMethod: String = "",
    val refundProofImageUri: String = "",
    val returnedItemsJson: String = "",
    val refundedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val settledAt: Long? = null
)

/**
 * Parsed line item from a SalesOrderEntity's itemsJson or returnedItemsJson.
 */
data class SalesOrderLineItem(
    val productId: String = "",
    val sku: String = "",
    val name: String,
    val quantity: Int,
    val unitPriceUsd: Double
) {
    val lineTotalUsd: Double get() = quantity * unitPriceUsd
}

fun parseSalesOrderLineItems(order: SalesOrderEntity): List<SalesOrderLineItem> {
    if (order.itemsJson.isNotBlank()) {
        try {
            val arr = org.json.JSONArray(order.itemsJson)
            val result = mutableListOf<SalesOrderLineItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val qty = obj.optInt("quantity", 0)
                if (qty > 0) {
                    result.add(
                        SalesOrderLineItem(
                            productId = obj.optString("productId"),
                            sku = obj.optString("sku"),
                            name = obj.optString("name", "Item"),
                            quantity = qty,
                            unitPriceUsd = obj.optDouble("unitPrice", 0.0)
                        )
                    )
                }
            }
            if (result.isNotEmpty()) return result
        } catch (_: Exception) { }
    }
    // Fallback for legacy/sample orders that only have itemsSummary
    if (order.itemsSummary.isNotBlank()) {
        val parts = order.itemsSummary.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val parsed = parts.mapNotNull { part ->
            val match = Regex("^(\\d+)x\\s*(.+)$").find(part)
            if (match != null) {
                val qty = match.groupValues[1].toIntOrNull() ?: 1
                val name = match.groupValues[2].trim()
                qty to name
            } else {
                1 to part
            }
        }
        val totalQty = parsed.sumOf { it.first }.coerceAtLeast(1)
        val avgUnit = order.subtotalUsd.takeIf { it > 0 }?.div(totalQty) ?: (order.totalAmountUsd / totalQty)
        return parsed.map { (qty, name) ->
            SalesOrderLineItem(name = name, quantity = qty, unitPriceUsd = avgUnit)
        }
    }
    return emptyList()
}

fun parseReturnedQuantitiesMap(returnedItemsJson: String): Map<String, Int> {
    if (returnedItemsJson.isBlank()) return emptyMap()
    return try {
        val arr = org.json.JSONArray(returnedItemsJson)
        val map = mutableMapOf<String, Int>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val key = obj.optString("productId").ifBlank { obj.optString("name") }
            val qty = obj.optInt("quantity", 0)
            if (key.isNotBlank() && qty > 0) {
                map[key] = (map[key] ?: 0) + qty
            }
        }
        map
    } catch (_: Exception) {
        emptyMap()
    }
}
