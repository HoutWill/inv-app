package com.example.inventoryapp.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.data.local.entity.StockTransactionEntity
import com.example.inventoryapp.ui.theme.*
import kotlin.math.abs

/**
 * Modern Swipe-Up Bottom-Sheet Modal for "Stock Transfers" (formerly Movement List).
 * Features clean header (soft-indigo swap icon badge + bold title + "+ Add" button + close button),
 * filter pills (All, Inbound, Outbound), minimal 3-column cards,
 * and progressive disclosure detail inspection on item tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockTransfersBottomSheet(
    transactions: List<StockTransactionEntity>,
    products: List<ProductEntity> = emptyList(),
    isDarkTheme: Boolean = false,
    onAddTransfer: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedTransaction by remember { mutableStateOf<StockTransactionEntity?>(null) }

    val filteredList = remember(transactions, selectedFilter) {
        when (selectedFilter) {
            "Inbound" -> transactions.filter { it.quantityDelta > 0 }
            "Outbound" -> transactions.filter { it.quantityDelta < 0 }
            else -> transactions
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = if (isDarkTheme) NawanoDark200 else MaterialTheme.colorScheme.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                width = 44.dp,
                height = 5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        },
        tonalElevation = 6.dp,
        modifier = Modifier.navigationBarsPadding().imePadding()
    ) {
        AnimatedContent(
            targetState = selectedTransaction,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally { width -> width / 2 } + fadeIn())
                        .togetherWith(slideOutHorizontally { width -> -width / 2 } + fadeOut())
                } else {
                    (slideInHorizontally { width -> -width / 2 } + fadeIn())
                        .togetherWith(slideOutHorizontally { width -> width / 2 } + fadeOut())
                }
            },
            label = "StockTransfersSheetContent"
        ) { detailTx ->
            if (detailTx != null) {
                // Progressive Disclosure: Detail View
                StockTransactionDetailView(
                    transaction = detailTx,
                    matchedProduct = products.find { it.sku.equals(detailTx.sku, ignoreCase = true) },
                    isDarkTheme = isDarkTheme,
                    sheetTitle = "Stock Transfers",
                    onBack = { selectedTransaction = null },
                    onDismiss = onDismiss
                )
            } else {
                // Main List View
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp)
                ) {
                    // Clean Header: Left swap-arrows badge + bold title + "+ Add" button + close button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Surface(
                                color = if (isDarkTheme) Color(0xFF312E81) else Color(0xFFE0E7FF),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.SwapHoriz,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Stock Transfers",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Purple "+ Add" pill button
                            Surface(
                                onClick = onAddTransfer,
                                shape = CircleShape,
                                color = NawanoPrimary200,
                                modifier = Modifier.height(34.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Add",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filter Pills: All (count), Inbound, Outbound
                    val filterPills = listOf(
                        "All (${transactions.size})",
                        "Inbound",
                        "Outbound"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        filterPills.forEach { pillLabel ->
                            val isSelected = when {
                                pillLabel.startsWith("All") -> selectedFilter == "All"
                                else -> selectedFilter == pillLabel
                            }
                            Surface(
                                onClick = {
                                    selectedFilter = if (pillLabel.startsWith("All")) "All" else pillLabel
                                },
                                shape = CircleShape,
                                color = if (isSelected) Color(0xFF6C63FF) else MaterialTheme.colorScheme.surface,
                                border = if (isSelected) null else BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                ),
                                shadowElevation = if (isSelected) 3.dp else 1.dp
                            ) {
                                Text(
                                    text = pillLabel,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                    ),
                                    color = if (isSelected) Color.White else Color(0xFF5E6582),
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Minimal 3-Column Item Cards List
                    if (filteredList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No transfers found for $selectedFilter",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 520.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredList, key = { it.id }) { tx ->
                                val isOutbound = tx.quantityDelta < 0
                                val unitsCount = abs(tx.quantityDelta)
                                val metadataLine = if (isOutbound) {
                                    "Outbound • $unitsCount Units"
                                } else {
                                    "Inbound • $unitsCount Units"
                                }

                                MinimalStockItemCard(
                                    transaction = tx,
                                    metadataLine = metadataLine,
                                    isDarkTheme = isDarkTheme,
                                    onClick = { selectedTransaction = tx }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
