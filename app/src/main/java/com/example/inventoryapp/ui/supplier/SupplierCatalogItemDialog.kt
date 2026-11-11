package com.example.inventoryapp.ui.supplier

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.ui.theme.*

/**
 * Dialog to register a new product into the Supplier's wholesale catalog.
 */
@Composable
fun AddCatalogProductDialog(
    initialSku: String = "",
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit,
    onRequestScanSku: () -> Unit = {},
    onSave: (ProductEntity) -> Unit
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight
    val fieldBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFCBD5E1)

    var name by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf(initialSku.ifBlank { "SKU-${(1000..9999).random()}" }) }
    var category by remember { mutableStateOf("Seafood") }
    var costPrice by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("Kg") }

    LaunchedEffect(initialSku) {
        if (initialSku.isNotBlank()) {
            sku = initialSku
        }
    }

    AlertDialog(
        containerColor = cardBg,
        onDismissRequest = onDismiss,
        title = {
            Text("Add to Wholesale Catalog", fontWeight = FontWeight.Bold, color = textPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name", color = textSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedBorderColor = NawanoPrimary200,
                        unfocusedBorderColor = fieldBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("SKU Code", color = textSecondary) },
                        trailingIcon = {
                            IconButton(onClick = onRequestScanSku) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan SKU Barcode",
                                    tint = NawanoPrimary200,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedBorderColor = NawanoPrimary200,
                            unfocusedBorderColor = fieldBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category", color = textSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedBorderColor = NawanoPrimary200,
                            unfocusedBorderColor = fieldBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costPrice,
                        onValueChange = { costPrice = it },
                        label = { Text("Supply Price ($)", color = textSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedBorderColor = NawanoPrimary200,
                            unfocusedBorderColor = fieldBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Available Qty", color = textSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedBorderColor = NawanoPrimary200,
                            unfocusedBorderColor = fieldBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cost = costPrice.toDoubleOrNull() ?: 0.0
                    val qty = quantity.toIntOrNull() ?: 0
                    val product = ProductEntity(
                        sku = sku.ifBlank { "SKU-${System.currentTimeMillis() % 10000}" },
                        name = name.ifBlank { "Wholesale Item" },
                        category = category.ifBlank { "General" },
                        costPrice = cost,
                        sellingPrice = cost * 1.3,
                        quantity = qty,
                        unit = unit
                    )
                    onSave(product)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textSecondary)
            }
        }
    )
}

/**
 * Dialog to edit an existing wholesale catalog item.
 */
@Composable
fun EditCatalogProductDialog(
    product: ProductEntity,
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight
    val fieldBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFCBD5E1)

    var name by remember { mutableStateOf(product.name) }
    var costPrice by remember { mutableStateOf(product.costPrice.toString()) }
    var quantity by remember { mutableStateOf(product.quantity.toString()) }

    AlertDialog(
        containerColor = cardBg,
        onDismissRequest = onDismiss,
        title = {
            Text("Edit ${product.sku}", fontWeight = FontWeight.Bold, color = textPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name", color = textSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedBorderColor = NawanoPrimary200,
                        unfocusedBorderColor = fieldBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costPrice,
                        onValueChange = { costPrice = it },
                        label = { Text("Supply Price ($)", color = textSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedBorderColor = NawanoPrimary200,
                            unfocusedBorderColor = fieldBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Stock Qty", color = textSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedBorderColor = NawanoPrimary200,
                            unfocusedBorderColor = fieldBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cost = costPrice.toDoubleOrNull() ?: product.costPrice
                    val qty = quantity.toIntOrNull() ?: product.quantity
                    onSave(product.copy(name = name, costPrice = cost, quantity = qty, updatedAt = System.currentTimeMillis()))
                },
                colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textSecondary)
            }
        }
    )
}
