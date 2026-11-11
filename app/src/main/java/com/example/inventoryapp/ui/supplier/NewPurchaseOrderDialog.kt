package com.example.inventoryapp.ui.supplier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderItemEntity
import com.example.inventoryapp.data.local.entity.SupplierEntity
import com.example.inventoryapp.ui.theme.*
import java.util.Locale

data class DraftOrderItem(
    val sku: String,
    val name: String,
    val quantity: Int,
    val unitCost: Double,
    val productId: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPurchaseOrderDialog(
    initialSupplier: SupplierEntity?,
    suppliers: List<SupplierEntity>,
    products: List<ProductEntity>,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    onDismiss: () -> Unit,
    onSubmit: (
        supplier: SupplierEntity,
        title: String,
        picSender: String,
        items: List<PurchaseOrderItemEntity>,
        ownerNote: String,
        estArrival: String
    ) -> Unit
) {
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight
    val surfaceColor = if (isDarkTheme) NawanoDark200 else Color.White
    val cardColor = if (isDarkTheme) NawanoDarkCard else Color(0xFFF8FAFC)
    val outlineColor = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)

    var selectedSupplier by remember { mutableStateOf(initialSupplier ?: suppliers.firstOrNull()) }
    var orderTitle by remember { mutableStateOf("") }
    var picSender by remember { mutableStateOf(selectedSupplier?.contactName ?: "Leslie Alexander") }
    var estArrival by remember { mutableStateOf("09:30 - 11:30 AM") }
    var ownerNote by remember { mutableStateOf("") }

    // Draft items
    val draftItems = remember {
        mutableStateListOf<DraftOrderItem>().apply {
            // If the supplier has products, prefill with one or low-stock items
            val supplierProducts = products.filter { it.supplierId == selectedSupplier?.id }
            if (supplierProducts.isNotEmpty()) {
                val first = supplierProducts.first()
                add(
                    DraftOrderItem(
                        sku = first.sku,
                        name = first.name,
                        quantity = if (first.reorderQty > 0) first.reorderQty else 10,
                        unitCost = first.costPrice,
                        productId = first.id
                    )
                )
            } else {
                add(
                    DraftOrderItem(
                        sku = "CRB-0603",
                        name = "King Crab Cluster (Toronto Pack)",
                        quantity = 15,
                        unitCost = 35.00
                    )
                )
            }
        }
    }

    // New item inputs
    var newSku by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var newQty by remember { mutableStateOf("10") }
    var newCost by remember { mutableStateOf("25.00") }

    AlertDialog(
        containerColor = surfaceColor,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Inbound Purchase Order",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Supplier selector
                Text(text = "Target Supplier", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    suppliers.forEach { s ->
                        val isSelected = selectedSupplier?.id == s.id
                        val itemBg = if (isSelected) {
                            if (isDarkTheme) NawanoPrimary200.copy(alpha = 0.25f) else Color(0xFFEDE9FE)
                        } else {
                            cardColor
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = itemBg,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) NawanoPrimary200 else outlineColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSupplier = s
                                    if (s.contactName.isNotBlank()) picSender = s.contactName
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = s.name,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) NawanoPrimary200 else textPrimary
                                )
                                if (s.contactName.isNotBlank()) {
                                    Text(text = "PIC: ${s.contactName}", fontSize = 11.sp, color = textSecondary)
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = orderTitle,
                    onValueChange = { orderTitle = it },
                    label = { Text("Order Title / Headline") },
                    placeholder = { Text("0603 - Items crab in from Toronto") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = picSender,
                        onValueChange = { picSender = it },
                        label = { Text("PIC Sender") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = estArrival,
                        onValueChange = { estArrival = it },
                        label = { Text("Est. Arrival") },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = outlineColor)

                // Order items
                Text(
                    text = "Shipment Items (${draftItems.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                draftItems.forEachIndexed { idx, item ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = cardColor,
                        border = BorderStroke(1.dp, outlineColor.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                Text(
                                    text = "SKU: ${item.sku} • ${item.quantity} units @ $${String.format(Locale.US, "%.2f", item.unitCost)}",
                                    fontSize = 11.sp,
                                    color = textSecondary
                                )
                            }
                            IconButton(
                                onClick = { draftItems.removeAt(idx) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Add item inputs
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cardColor,
                    border = BorderStroke(1.dp, outlineColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "Add Item to PO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NawanoPrimary200)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = newSku,
                                onValueChange = { newSku = it },
                                placeholder = { Text("SKU") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                placeholder = { Text("Name") },
                                singleLine = true,
                                modifier = Modifier.weight(2f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = newQty,
                                onValueChange = { newQty = it },
                                placeholder = { Text("Qty") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newCost,
                                onValueChange = { newCost = it },
                                placeholder = { Text("Cost") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    val q = newQty.toIntOrNull() ?: 1
                                    val c = newCost.toDoubleOrNull() ?: 0.0
                                    if (newName.isNotBlank()) {
                                        draftItems.add(
                                            DraftOrderItem(
                                                sku = newSku.ifBlank { "SKU-${System.currentTimeMillis() % 1000}" },
                                                name = newName.trim(),
                                                quantity = q,
                                                unitCost = c
                                            )
                                        )
                                        newSku = ""
                                        newName = ""
                                        newQty = "10"
                                        newCost = "25.00"
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200),
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = ownerNote,
                    onValueChange = { ownerNote = it },
                    label = { Text("Internal Instructions / Note") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sup = selectedSupplier ?: return@Button
                    val finalItems = draftItems.map { d ->
                        PurchaseOrderItemEntity(
                            orderId = "",
                            productId = d.productId,
                            sku = d.sku,
                            productName = d.name,
                            quantity = d.quantity,
                            unitCost = d.unitCost
                        )
                    }
                    val title = orderTitle.ifBlank { "Inbound Delivery from ${sup.name}" }
                    onSubmit(sup, title, picSender, finalItems, ownerNote, estArrival)
                },
                enabled = selectedSupplier != null && draftItems.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200)
            ) {
                Text("Submit Purchase Order")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
