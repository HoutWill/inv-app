package com.example.inventoryapp.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.data.local.entity.StockTransactionEntity
import com.example.inventoryapp.ui.theme.NawanoPrimary200
import java.util.Locale

@Composable
fun StockNotificationsDialog(
    lowStockProducts: List<ProductEntity>,
    recentTransactions: List<StockTransactionEntity>,
    onDismiss: () -> Unit,
    onRestock: (ProductEntity, Int) -> Unit,
    onViewAllCatalog: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("Alerts") } // "Alerts" or "Activity"
    val outOfStockCount = lowStockProducts.count { it.quantity <= 0 }
    val warningCount = lowStockProducts.count { it.quantity > 0 }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 16.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (lowStockProducts.isNotEmpty()) Color(0xFFEF4444).copy(alpha = 0.12f)
                            else NawanoPrimary200.copy(alpha = 0.15f),
                            border = BorderStroke(
                                1.dp,
                                if (lowStockProducts.isNotEmpty()) Color(0xFFEF4444).copy(alpha = 0.3f)
                                else NawanoPrimary200.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (lowStockProducts.isNotEmpty()) Icons.Default.Warning else Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = if (lowStockProducts.isNotEmpty()) Color(0xFFEF4444) else NawanoPrimary200,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Notifications & Alerts",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (lowStockProducts.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFEF4444)
                                    ) {
                                        Text(
                                            text = "${lowStockProducts.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (lowStockProducts.isNotEmpty()) "$outOfStockCount out of stock • $warningCount low stock"
                                else "All stock levels currently healthy",
                                style = MaterialTheme.typography.bodySmall,
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Filter Tabs: Stock Alerts vs Activity History
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "Alerts",
                        onClick = { selectedFilter = "Alerts" },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Stock Alerts", fontWeight = FontWeight.Bold)
                                if (lowStockProducts.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "(${lowStockProducts.size})",
                                        color = if (selectedFilter == "Alerts") MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFFEF4444),
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (lowStockProducts.isNotEmpty()) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    FilterChip(
                        selected = selectedFilter == "Activity",
                        onClick = { selectedFilter = "Activity" },
                        label = {
                            Text("Activity (${recentTransactions.size})", fontWeight = FontWeight.Bold)
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Content Viewport
                Box(modifier = Modifier.weight(1f)) {
                    if (selectedFilter == "Alerts") {
                        if (lowStockProducts.isEmpty()) {
                            // Healthy state
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "All Stock Levels Healthy!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "No products are currently under reorder thresholds.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(lowStockProducts, key = { it.id }) { product ->
                                    val isOutOfStock = product.quantity <= 0
                                    val alertColor = if (isOutOfStock) Color(0xFFEF4444) else Color(0xFFF59E0B)

                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        border = BorderStroke(1.dp, alertColor.copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = alertColor.copy(alpha = 0.15f),
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = if (isOutOfStock) Icons.Default.ErrorOutline else Icons.Default.WarningAmber,
                                                            contentDescription = null,
                                                            tint = alertColor,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }

                                                Column {
                                                    Text(
                                                        text = product.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = product.sku,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = NawanoPrimary200
                                                        )
                                                        Text(
                                                            text = "•",
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = if (isOutOfStock) "Out of Stock (0)"
                                                            else "${product.quantity} left (Min: ${product.reorderLevel})",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = alertColor
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            // Quick Restock Button
                                            Button(
                                                onClick = { onRestock(product, 10) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = NawanoPrimary200,
                                                    contentColor = Color.White
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("+10", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Activity Tab
                        if (recentTransactions.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No Recent Activity Recorded",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(recentTransactions.take(20), key = { it.id }) { tx ->
                                    val isStockIn = tx.transactionType == "STOCK_IN"
                                    val isStockOut = tx.transactionType == "STOCK_OUT"
                                    val txColor = if (isStockIn) Color(0xFF10B981) else if (isStockOut) Color(0xFFEF4444) else NawanoPrimary200

                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = txColor.copy(alpha = 0.15f),
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = if (isStockIn) Icons.Default.ArrowDownward
                                                            else if (isStockOut) Icons.Default.ArrowUpward
                                                            else Icons.Default.Add,
                                                            contentDescription = null,
                                                            tint = txColor,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }

                                                Column {
                                                    Text(
                                                        text = tx.productName,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "${tx.sku} • ${tx.note ?: tx.transactionType}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Text(
                                                text = if (tx.quantityDelta > 0) "+${tx.quantityDelta}" else "${tx.quantityDelta}",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = txColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Dismiss")
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onViewAllCatalog()
                        },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200)
                    ) {
                        Text("View Catalog", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
