package com.example.inventoryapp.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.ui.theme.NawanoPrimary200
import com.example.inventoryapp.ui.theme.rememberSafeModalBottomSheetState
import com.example.inventoryapp.util.AppCurrency
import com.example.inventoryapp.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Swipe-up modal bottom sheet to park/hold an active POS cart.
 * Complies strictly with project UI rules: Material 3 ModalBottomSheet, 28dp top corners,
 * drag handle, imePadding/navigationBarsPadding, zero center dialogs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosHoldCartBottomSheet(
    cart: Map<String, Int>,
    products: List<ProductEntity>,
    currency: AppCurrency = AppCurrency.USD,
    exchangeRate: Double = CurrencyFormatter.DEFAULT_EXCHANGE_RATE,
    onDismiss: () -> Unit,
    onConfirmHold: (label: String, note: String) -> Unit
) {
    val sheetState = rememberSafeModalBottomSheetState(skipPartiallyExpanded = true)

    val itemCount = cart.values.sum()
    val totalAmountUsd = cart.entries.sumOf { (prodId, qty) ->
        val prod = products.find { it.id == prodId }
        (prod?.sellingPrice ?: 0.0) * qty
    }

    val defaultLabel = "Order #${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}"
    var labelInput by remember { mutableStateOf(defaultLabel) }
    var noteInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                width = 44.dp,
                height = 5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.BookmarkAdd,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Hold / Park Order",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Save current cart to resume later",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Cart Summary Preview Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.ShoppingBag,
                            contentDescription = null,
                            tint = NawanoPrimary200,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "$itemCount items in cart",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${cart.size} unique products",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = CurrencyFormatter.format(totalAmountUsd, currency, exchangeRate),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = NawanoPrimary200
                        )
                    }
                }
            }

            // Inputs: Label / Customer Name
            OutlinedTextField(
                value = labelInput,
                onValueChange = { labelInput = it },
                label = { Text("Order Label / Table / Customer Name") },
                placeholder = { Text("e.g. Table 4, John Doe, Takeaway") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Inputs: Optional Note
            OutlinedTextField(
                value = noteInput,
                onValueChange = { noteInput = it },
                label = { Text("Notes (Optional)") },
                placeholder = { Text("e.g. Customer stepped out to get cash") },
                singleLine = false,
                maxLines = 2,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(4.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Cancel", fontSize = 14.sp)
                }

                Button(
                    onClick = {
                        onConfirmHold(labelInput.ifBlank { defaultLabel }, noteInput)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Hold & Clear Cart",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
