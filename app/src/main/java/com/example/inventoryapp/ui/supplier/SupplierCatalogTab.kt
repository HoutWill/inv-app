package com.example.inventoryapp.ui.supplier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.ui.theme.*
import java.util.Locale

/**
 * Wholesale Catalog View for the Supplier Portal, showing supplied goods and wholesale pricing.
 */
@Composable
fun SupplierCatalogTab(
    products: List<ProductEntity>,
    isDarkTheme: Boolean = false,
    isSortAscending: Boolean,
    onToggleSort: () -> Unit,
    onAddProduct: () -> Unit = {},
    onEditProduct: (ProductEntity) -> Unit,
    onDeleteProduct: (ProductEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Header Row: Count & Actions (Add + Sort)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${products.size} Wholesale Products",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Add Product Quick Button
                    Surface(
                        onClick = onAddProduct,
                        shape = RoundedCornerShape(12.dp),
                        color = NawanoPrimary200
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Product",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Add",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Sort Button
                    Surface(
                        onClick = onToggleSort,
                        shape = RoundedCornerShape(12.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder),
                        shadowElevation = if (isDarkTheme) 0.dp else 0.5.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Sort",
                                tint = NawanoPrimary200,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (isSortAscending) "Price ↑" else "Recent ↓",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        }
                    }
                }
            }
        }

        if (products.isEmpty()) {
            item {
                EmptyZeroState(
                    title = "Catalog Empty",
                    subtitle = "Tap '+' to add products that you supply to Middleman HQ.",
                    icon = Icons.Default.Category,
                    isDarkTheme = isDarkTheme
                )
            }
        } else {
            items(products, key = { it.id }) { product ->
                SupplierCatalogProductCard(
                    product = product,
                    isDarkTheme = isDarkTheme,
                    onEdit = { onEditProduct(product) },
                    onDelete = { onDeleteProduct(product) }
                )
            }
        }
    }
}

@Composable
fun SupplierCatalogProductCard(
    product: ProductEntity,
    isDarkTheme: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isDarkTheme) 0.dp else 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDarkTheme) NawanoPrimary200.copy(alpha = 0.2f) else Color(0xFFF3E8FF)
                    ) {
                        Text(
                            text = product.category.ifBlank { "Wholesale" },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NawanoPrimary200,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = product.sku,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textSecondary
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Cost: $${String.format(Locale.US, "%.2f", product.costPrice)} / ${product.unit}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF047857)
                    )
                    Text(
                        text = "Stock: ${product.quantity} ${product.unit}",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Product",
                        tint = textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Product",
                        tint = if (isDarkTheme) Color(0xFFF87171) else Color(0xFFDC2626),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
