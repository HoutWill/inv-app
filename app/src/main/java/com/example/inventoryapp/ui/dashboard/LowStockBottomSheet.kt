package com.example.inventoryapp.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.data.local.entity.canRequestSupply
import com.example.inventoryapp.ui.theme.*
import com.example.inventoryapp.util.ProductThumbnail

/**
 * Modern Swipe-Up Bottom-Sheet Modal for "Low Stock" Alerts.
 * Displays exclusively products that are at or below their reorder threshold,
 * with clean amber warning header, filter pills, minimal 3-column cards,
 * and quick replenishment or supplier request actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LowStockBottomSheet(
    products: List<ProductEntity>,
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit,
    onProductClick: (ProductEntity) -> Unit,
    onRestockProduct: (ProductEntity, Int) -> Unit,
    onRequestSupply: ((ProductEntity) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Filter strictly to low stock products (quantity <= reorderLevel or quantity <= 5)
    val lowStockItems = remember(products) {
        products.filter { it.quantity <= it.reorderLevel || it.quantity <= 5 }
            .sortedBy { it.quantity }
    }

    var selectedFilter by remember { mutableStateOf("All") }

    val filteredList = remember(lowStockItems, selectedFilter) {
        when (selectedFilter) {
            "Out of Stock" -> lowStockItems.filter { it.quantity <= 0 }
            "Low Stock" -> lowStockItems.filter { it.quantity > 0 }
            else -> lowStockItems
        }
    }

    val outOfStockCount = remember(lowStockItems) { lowStockItems.count { it.quantity <= 0 } }
    val lowCount = remember(lowStockItems) { lowStockItems.count { it.quantity > 0 } }

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Clean Header: Amber warning triangle badge + bold title + close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (isDarkTheme) Color(0xFF451A03) else Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Low Stock Alerts",
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

            // Filter Pills: All, Out of Stock, Low Stock
            val filterPills = listOf(
                "All (${lowStockItems.size})",
                "Out of Stock ($outOfStockCount)",
                "Low Stock ($lowCount)"
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
                        pillLabel.startsWith("Out of Stock") -> selectedFilter == "Out of Stock"
                        pillLabel.startsWith("Low Stock") -> selectedFilter == "Low Stock"
                        else -> selectedFilter == pillLabel
                    }
                    Surface(
                        onClick = {
                            selectedFilter = when {
                                pillLabel.startsWith("All") -> "All"
                                pillLabel.startsWith("Out of Stock") -> "Out of Stock"
                                pillLabel.startsWith("Low Stock") -> "Low Stock"
                                else -> pillLabel
                            }
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

            // Minimal 3-Column Low Stock Cards
            if (filteredList.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDarkTheme) NawanoDarkCard else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(
                        1.dp,
                        if (isDarkTheme) NawanoDarkOutline else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = NawanoEmerald.copy(alpha = 0.15f),
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = NawanoEmerald,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "All Stock Levels Healthy",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No items are currently below reorder levels or out of stock.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { product ->
                        LowStockItemCard(
                            product = product,
                            isDarkTheme = isDarkTheme,
                            onClick = { onProductClick(product) },
                            onRestock = { onRestockProduct(product, 10) },
                            onRequestSupply = if (onRequestSupply != null && product.canRequestSupply) {
                                { onRequestSupply(product) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

/**
 * Minimal 3-Column Item Card for Low Stock alerts:
 * - Left Column: Square rounded product thumbnail (48x48px, radius: 12dp)
 * - Middle Column: Line 1 (Clean Title), Line 2 (Reorder threshold & category)
 * - Right Column: Top (Bold quantity remaining in amber/red), Bottom (Pill button: "Request" or "+10 Restock")
 */
@Composable
private fun LowStockItemCard(
    product: ProductEntity,
    isDarkTheme: Boolean = false,
    onClick: () -> Unit,
    onRestock: () -> Unit,
    onRequestSupply: (() -> Unit)? = null
) {
    val isOutOfStock = product.quantity <= 0
    val countColor = if (isOutOfStock) Color(0xFFDC2626) else Color(0xFFD97706)
    val countText = if (isOutOfStock) "0 left" else "${product.quantity} left"

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
            // 1. Left Column: Product Thumbnail (48x48px, radius: 12dp)
            ProductThumbnail(
                sku = product.sku,
                name = product.name,
                category = product.category,
                imageUri = product.imageUri,
                supplier = null,
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 2. Middle Column: Title + Reorder Level Metadata
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "Reorder at ${product.reorderLevel} ${product.unit} • ${product.category}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 3. Right Column: Top (Remaining quantity), Bottom (Pill button)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = countText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.5.sp,
                    color = countColor
                )

                if (onRequestSupply != null) {
                    Surface(
                        onClick = onRequestSupply,
                        shape = RoundedCornerShape(12.dp),
                        color = NawanoPrimary200,
                        modifier = Modifier.defaultMinSize(minWidth = 64.dp, minHeight = 24.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Request",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    Surface(
                        onClick = onRestock,
                        shape = RoundedCornerShape(12.dp),
                        color = NawanoPrimary200,
                        modifier = Modifier.defaultMinSize(minWidth = 64.dp, minHeight = 24.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+10 Restock",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
