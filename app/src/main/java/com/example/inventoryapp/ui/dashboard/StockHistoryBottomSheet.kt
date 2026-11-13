package com.example.inventoryapp.ui.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.data.local.entity.StockTransactionEntity
import com.example.inventoryapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modern Swipe-Up Bottom-Sheet Modal for "Stock History" (formerly Stock Audit Logs).
 * Features clean header (icon badge + bold title + close button),
 * filter pills (All, Inflows, Outflows), minimal 3-column cards,
 * and progressive disclosure detail inspection on item tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockHistoryBottomSheet(
    transactions: List<StockTransactionEntity>,
    products: List<ProductEntity> = emptyList(),
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedTransaction by remember { mutableStateOf<StockTransactionEntity?>(null) }

    val filteredList = remember(transactions, selectedFilter) {
        when (selectedFilter) {
            "Inflows (+)" -> transactions.filter { it.quantityDelta > 0 }
            "Outflows (-)" -> transactions.filter { it.quantityDelta < 0 }
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
            label = "StockHistorySheetContent"
        ) { detailTx ->
            if (detailTx != null) {
                // Progressive Disclosure: Detail View
                StockTransactionDetailView(
                    transaction = detailTx,
                    matchedProduct = products.find { it.sku.equals(detailTx.sku, ignoreCase = true) },
                    isDarkTheme = isDarkTheme,
                    sheetTitle = "Stock History",
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
                    // Clean Header: Green clipboard-check badge + bold title + close button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.FactCheck,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Stock History",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filter Pills: All (count), Inflows (+), Outflows (-)
                    val filterPills = listOf(
                        "All (${transactions.size})",
                        "Inflows (+)",
                        "Outflows (-)"
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
                                text = "No records found for $selectedFilter",
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
                                val matchedProd = products.find { it.sku.equals(tx.sku, ignoreCase = true) }
                                val currentStock = matchedProd?.quantity ?: tx.newQuantity
                                val adjustmentType = when {
                                    tx.note?.contains("Catalog", ignoreCase = true) == true -> "Catalog Adjustment"
                                    tx.note?.contains("Restock", ignoreCase = true) == true -> "Restock Inbound"
                                    tx.note?.contains("Damage", ignoreCase = true) == true -> "Damage Deduction"
                                    tx.transactionType.contains("IN", ignoreCase = true) -> "Stock Inflow"
                                    tx.transactionType.contains("OUT", ignoreCase = true) -> "Stock Outflow"
                                    else -> "Catalog Adjustment"
                                }
                                val metadataLine = "$adjustmentType • $currentStock in stock"

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

/**
 * Minimal 3-Column List Item Card matching design specifications:
 * - Left Column: Rounded square icon badge (44x44px, radius: 12dp) with soft coral/red outflow or soft green inflow
 * - Middle Column: Line 1 (Bold item name only, prefixes stripped), Line 2 (Clean metadata line)
 * - Right Column: Top (Bold signed delta: -1 in dark crimson or +1 in emerald green), Bottom (Compact timestamp)
 * - No badges on card face (Deducted, Completed, etc. removed)
 */
@Composable
fun MinimalStockItemCard(
    transaction: StockTransactionEntity,
    metadataLine: String,
    isDarkTheme: Boolean = false,
    onClick: () -> Unit
) {
    val isOutflow = transaction.quantityDelta < 0
    val deltaSignedText = if (transaction.quantityDelta > 0) "+${transaction.quantityDelta}" else "${transaction.quantityDelta}"
    val deltaColor = if (isOutflow) Color(0xFFDC2626) else Color(0xFF10B981)

    val iconBg = if (isOutflow) {
        if (isDarkTheme) Color(0xFF451A1A) else Color(0xFFFEE2E2)
    } else {
        if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5)
    }

    val iconTint = if (isOutflow) Color(0xFFEF4444) else Color(0xFF10B981)

    val cleanTitle = cleanItemName(transaction.productName)

    val timeFormatted = remember(transaction.timestamp) {
        val sdf = SimpleDateFormat("d MMM, HH:mm", Locale.US)
        sdf.format(Date(transaction.timestamp))
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkTheme) NawanoDarkCard else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isDarkTheme) NawanoDarkOutline else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        ),
        shadowElevation = if (isDarkTheme) 0.dp else 1.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Rounded square icon badge (44x44px, radius: 12dp)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconBg,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isOutflow) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                        contentDescription = if (isOutflow) "Outflow" else "Inflow",
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Middle Column: Line 1 (Bold item name), Line 2 (Clean metadata)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = cleanTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = metadataLine,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right Column: Top (Bold signed delta), Bottom (Compact timestamp)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = deltaSignedText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = deltaColor
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Strips prefixes like "Stock Out:", "Stock In:", "Restock:", etc.
 */
fun cleanItemName(rawName: String): String {
    return rawName
        .replace(Regex("^Stock\\s*(Out|In):\\s*", RegexOption.IGNORE_CASE), "")
        .replace(Regex("^Restock:\\s*", RegexOption.IGNORE_CASE), "")
        .replace(Regex("^Adjustment:\\s*", RegexOption.IGNORE_CASE), "")
        .replace(Regex("^PO-\\d+.*?-\\s*", RegexOption.IGNORE_CASE), "")
        .trim()
        .ifBlank { rawName.trim() }
}

/**
 * Progressive Disclosure Detail View for technical transaction inspection:
 * full barcode SKU, transaction ID #TX-..., person in charge, notes, and stock impact.
 */
@Composable
fun StockTransactionDetailView(
    transaction: StockTransactionEntity,
    matchedProduct: ProductEntity?,
    isDarkTheme: Boolean = false,
    sheetTitle: String = "Stock History",
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isOutflow = transaction.quantityDelta < 0
    val deltaSignedText = if (transaction.quantityDelta > 0) "+${transaction.quantityDelta}" else "${transaction.quantityDelta}"
    val deltaColor = if (isOutflow) Color(0xFFDC2626) else Color(0xFF10B981)
    val shortTx = "#TX-${transaction.id.takeLast(8).uppercase()}"

    val fullDate = remember(transaction.timestamp) {
        val sdf = SimpleDateFormat("EEEE, d MMMM yyyy • HH:mm:ss", Locale.US)
        sdf.format(Date(transaction.timestamp))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 36.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Navigation Row: Back button + Title + Dismiss
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Transaction Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

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

        Spacer(modifier = Modifier.height(12.dp))

        // Hero Summary Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDarkTheme) NawanoDarkCard else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(
                1.dp,
                if (isDarkTheme) NawanoDarkOutline else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isOutflow) {
                        if (isDarkTheme) Color(0xFF451A1A) else Color(0xFFFEE2E2)
                    } else {
                        if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5)
                    },
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isOutflow) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                            contentDescription = null,
                            tint = if (isOutflow) Color(0xFFEF4444) else Color(0xFF10B981),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cleanItemName(transaction.productName),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = fullDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = deltaSignedText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = deltaColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Technical Specs Grid
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDarkTheme) NawanoDarkCard else MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                1.dp,
                if (isDarkTheme) NawanoDarkOutline else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Transaction ID row with Copy
                DetailRow(
                    label = "Transaction ID",
                    value = shortTx,
                    trailingAction = {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Transaction ID", transaction.id))
                                Toast.makeText(context, "Copied ID to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy ID",
                                tint = NawanoPrimary200,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                // Barcode SKU row with Monospace + Copy
                DetailRow(
                    label = "Barcode SKU",
                    value = transaction.sku,
                    isMonospace = true,
                    trailingAction = {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Barcode SKU", transaction.sku))
                                Toast.makeText(context, "Copied SKU to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.QrCode,
                                contentDescription = "Copy SKU",
                                tint = NawanoPrimary200,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                // Movement Type
                DetailRow(
                    label = "Movement Type",
                    value = if (transaction.quantityDelta > 0) "Inbound / Stock Receipt" else "Outbound / Stock Adjustment"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                // Stock Impact
                val finalStock = matchedProduct?.quantity ?: transaction.newQuantity
                DetailRow(
                    label = "Stock Impact",
                    value = "Prev: ${transaction.previousQuantity}  →  Current: $finalStock Units"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                // Person in Charge
                DetailRow(
                    label = "Person in Charge (PIC)",
                    value = "Anthony • Middle Man (Nawano HQ)"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                // Note / Reason
                DetailRow(
                    label = "Operator Notes",
                    value = transaction.note?.ifBlank { "Physical verification / system record" } ?: "Direct inventory adjustment"
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Return button
        Button(
            onClick = onBack,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NawanoPrimary200
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "Back to $sheetTitle",
                fontWeight = FontWeight.Bold,
                fontSize = 14.5.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isMonospace: Boolean = false,
    trailingAction: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (trailingAction != null) {
            trailingAction()
        }
    }
}
