package com.example.inventoryapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class SupplierIssueStatus {
    OPEN,
    ACKNOWLEDGED,
    REPLACEMENT_SENT,
    RESOLVED,
    DISMISSED
}

enum class IssueSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Issue report submitted by a Middleman against a Supplier or Purchase Order.
 * Surfaced in the Supplier Portal ("Issues from Middleman") so suppliers can
 * acknowledge, dispatch replacements, or resolve disputes with full audit tracking.
 */
@Entity(
    tableName = "supplier_issues",
    indices = [
        Index(value = ["supplier_id"]),
        Index(value = ["middleman_id"]),
        Index(value = ["order_id"]),
        Index(value = ["status"])
    ]
)
data class SupplierIssueEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "issue_code") val issueCode: String,
    @ColumnInfo(name = "order_id") val orderId: String? = null,
    @ColumnInfo(name = "order_number") val orderNumber: String = "",
    @ColumnInfo(name = "supplier_id") val supplierId: String,
    @ColumnInfo(name = "supplier_name") val supplierName: String,
    @ColumnInfo(name = "middleman_id") val middlemanId: String = "user_middleman",
    @ColumnInfo(name = "middleman_name") val middlemanName: String,
    @ColumnInfo(name = "middleman_code") val middlemanCode: String = "MID-1001",
    @ColumnInfo(name = "issue_type") val issueType: String,
    val severity: IssueSeverity = IssueSeverity.HIGH,
    @ColumnInfo(name = "affected_item_name") val affectedItemName: String = "",
    @ColumnInfo(name = "affected_quantity") val affectedQuantity: Int = 0,
    val description: String,
    @ColumnInfo(name = "proof_image_uri") val proofImageUri: String = "",
    val status: SupplierIssueStatus = SupplierIssueStatus.OPEN,
    @ColumnInfo(name = "supplier_response") val supplierResponse: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "resolved_at") val resolvedAt: Long? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
