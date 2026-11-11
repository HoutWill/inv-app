package com.example.inventoryapp.ui.supplier

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.inventoryapp.data.local.entity.PurchaseOrderEntity
import com.example.inventoryapp.ui.theme.*

/**
 * Dialog to dispatch an approved Purchase Order, capturing carrier and tracking information.
 */
@Composable
fun DispatchOutboundDialog(
    order: PurchaseOrderEntity,
    initialTracking: String = "",
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit,
    onRequestScanTracking: () -> Unit = {},
    onConfirmDispatch: (tracking: String, carrier: String, eta: String, weight: String, note: String) -> Unit
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight
    val fieldBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFCBD5E1)

    val defaultTracking = remember(order.id, initialTracking) {
        if (initialTracking.isNotBlank()) initialTracking
        else if (order.trackingNumber.isNotBlank()) order.trackingNumber
        else "TRK-${(1000..9999).random()}"
    }
    var trackingNumber by remember { mutableStateOf(defaultTracking) }
    var carrier by remember { mutableStateOf(if (order.picSender.isNotBlank()) order.picSender else "ColdFreight Express") }
    var etaWindow by remember { mutableStateOf(if (order.estArrival.isNotBlank()) order.estArrival else "Today, 02:00 - 04:30 PM") }
    var weight by remember { mutableStateOf(if (order.totalWeight.isNotBlank()) order.totalWeight else "14.5 KG") }
    var dispatchNote by remember { mutableStateOf("Refrigerated freight. Temperature logged.") }

    LaunchedEffect(initialTracking) {
        if (initialTracking.isNotBlank()) {
            trackingNumber = initialTracking
        }
    }

    AlertDialog(
        containerColor = cardBg,
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    tint = NawanoPrimary200,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Dispatch ${order.orderNumber}",
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = trackingNumber,
                    onValueChange = { trackingNumber = it },
                    label = { Text("Waybill / Tracking Number", color = textSecondary) },
                    trailingIcon = {
                        IconButton(onClick = onRequestScanTracking) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan Waybill Barcode",
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
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = carrier,
                        onValueChange = { carrier = it },
                        label = { Text("Freight Carrier", color = textSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedBorderColor = NawanoPrimary200,
                            unfocusedBorderColor = fieldBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Gross Wt", color = textSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedBorderColor = NawanoPrimary200,
                            unfocusedBorderColor = fieldBorder
                        ),
                        modifier = Modifier.weight(0.8f)
                    )
                }

                OutlinedTextField(
                    value = etaWindow,
                    onValueChange = { etaWindow = it },
                    label = { Text("Estimated Arrival Window", color = textSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedBorderColor = NawanoPrimary200,
                        unfocusedBorderColor = fieldBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dispatchNote,
                    onValueChange = { dispatchNote = it },
                    label = { Text("Driver / Packaging Note", color = textSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedBorderColor = NawanoPrimary200,
                        unfocusedBorderColor = fieldBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmDispatch(trackingNumber, carrier, etaWindow, weight, dispatchNote)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4338CA)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Confirm Dispatch", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textSecondary)
            }
        }
    )
}
