package com.example.inventoryapp.ui.supplier

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderItemEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderStatus
import com.example.inventoryapp.data.local.entity.UserEntity
import com.example.inventoryapp.data.repository.OrderWithItems
import com.example.inventoryapp.ui.dashboard.FloatingNavItem
import com.example.inventoryapp.ui.scanner.BarcodeScannerScreen
import com.example.inventoryapp.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierOrdersScreen(
    user: UserEntity,
    orders: List<PurchaseOrderEntity> = emptyList(),
    products: List<ProductEntity> = emptyList(),
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    onToggleTheme: () -> Unit = {},
    onConfirmOrder: (String, String?) -> Unit = { _, _ -> },
    onRejectOrder: (String, String) -> Unit = { _, _ -> },
    onMarkDelivered: (orderId: String, trackingNumber: String?, carrier: String?, estArrival: String?, totalWeight: String?, note: String?) -> Unit = { _, _, _, _, _, _ -> },
    onCreateOrder: (title: String, quantity: Int, price: Double, estArrival: String) -> Unit = { _, _, _, _ -> },
    onUpdateOrder: (orderId: String, title: String, quantity: Int, price: Double) -> Unit = { _, _, _, _ -> },
    onDeleteOrder: (String) -> Unit = {},
    onAddProduct: (ProductEntity) -> Unit = {},
    onUpdateProduct: (ProductEntity) -> Unit = {},
    onDeleteProduct: (ProductEntity) -> Unit = {},
    onLoadOrderDetails: suspend (String) -> OrderWithItems? = { null },
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 0 = Orders view, 1 = Catalog (Items) view
    var activeViewTab by remember { mutableIntStateOf(0) }
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val filterTabs = listOf("All", "Pending", "Transit", "Done")

    // Modals & Sheets
    var showCreateDialog by remember { mutableStateOf(false) }
    var orderToEdit by remember { mutableStateOf<PurchaseOrderEntity?>(null) }
    var orderToDelete by remember { mutableStateOf<PurchaseOrderEntity?>(null) }
    var orderToDispatch by remember { mutableStateOf<PurchaseOrderEntity?>(null) }
    var orderToViewDetails by remember { mutableStateOf<PurchaseOrderEntity?>(null) }
    var detailedOrderWithItems by remember { mutableStateOf<OrderWithItems?>(null) }

    // Live Scanner & Search
    var showLiveCameraScanner by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSortAscending by remember { mutableStateOf(false) }

    // Catalog item modal
    var showAddCatalogItemDialog by remember { mutableStateOf(false) }
    var catalogItemToEdit by remember { mutableStateOf<ProductEntity?>(null) }

    // Metrics calculations
    val totalOrders = orders.size
    val totalSkus = remember(orders) { orders.map { it.orderNumber }.distinct().size }
    val totalQty = remember(orders) { orders.sumOf { it.totalQuantity } }
    val totalValue = remember(orders) { orders.sumOf { it.totalAmount } }

    val filteredOrders = remember(orders, selectedFilterIndex, isSortAscending, searchQuery) {
        val list = when (selectedFilterIndex) {
            1 -> orders.filter { it.status == PurchaseOrderStatus.PENDING }
            2 -> orders.filter { it.status == PurchaseOrderStatus.CONFIRMED || it.status == PurchaseOrderStatus.DELIVERED }
            3 -> orders.filter { it.status == PurchaseOrderStatus.RECEIVED }
            else -> orders
        }.filter {
            if (searchQuery.isBlank()) true
            else it.orderNumber.contains(searchQuery, ignoreCase = true) || it.title.contains(searchQuery, ignoreCase = true)
        }
        if (isSortAscending) list.sortedBy { it.totalAmount } else list.sortedByDescending { it.createdAt }
    }

    val filteredCatalogProducts = remember(products, searchQuery) {
        if (searchQuery.isBlank()) products
        else products.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.sku.contains(searchQuery, ignoreCase = true)
        }
    }

    val initials = remember(user.fullName) {
        val parts = user.fullName.trim().split(" ")
        if (parts.size >= 2) {
            "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
        } else {
            user.fullName.take(2).uppercase()
        }
    }

    // Full Camera Scanner View Overlay
    if (showLiveCameraScanner) {
        BarcodeScannerScreen(
            onBarcodeScanned = { scanned ->
                searchQuery = scanned
                showLiveCameraScanner = false
                Toast.makeText(context, "Matched SKU: $scanned", Toast.LENGTH_SHORT).show()
            },
            onClose = { showLiveCameraScanner = false }
        )
        return
    }

    val bgMain = if (isDarkTheme) NawanoDark400 else NawanoNeutral50
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgMain)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Top Bar (Matches Middleman InventoryTopBar design)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(NawanoGradientStart, NawanoGradientEnd)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Column {
                        Text(
                            text = user.fullName.replace(" (Supplier)", ""),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (activeViewTab == 0) "Supplier Portal • ${orders.size} Orders" else "Wholesale Catalog • ${products.size} SKUs",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Dark / Light Mode Toggle Button (Identical to Middleman)
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                            tint = if (isDarkTheme) Color(0xFFFBBF24) else textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Add Button (+)
                    IconButton(
                        onClick = {
                            if (activeViewTab == 0) showCreateDialog = true
                            else showAddCatalogItemDialog = true
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = "New Order",
                            tint = NawanoPrimary200,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    // Exit / Logout Button
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                // 1. Hero Summary Card (Violet Gradient matching Middleman)
                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Transparent,
                        shadowElevation = if (isDarkTheme) 0.dp else 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF6366F1),
                                            NawanoPrimary200,
                                            Color(0xFF9061F9)
                                        )
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White.copy(alpha = 0.2f),
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (activeViewTab == 0) Icons.Default.Layers else Icons.Default.Category,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (activeViewTab == 0) "Summary" else "Catalog Overview",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Spacer(Modifier.height(18.dp))

                                // Metrics
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (activeViewTab == 0) "Orders" else "Products",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White.copy(alpha = 0.75f)
                                        )
                                        Text(
                                            text = if (activeViewTab == 0) "$totalOrders" else "${products.size}",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "SKUs",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White.copy(alpha = 0.75f)
                                        )
                                        Text(
                                            text = "$totalSkus",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Qty",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White.copy(alpha = 0.75f)
                                        )
                                        Text(
                                            text = "$totalQty",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1.2f)) {
                                        Text(
                                            text = "Value",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White.copy(alpha = 0.75f)
                                        )
                                        Text(
                                            text = "$${String.format(Locale.US, "%,.0f", totalValue)}",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Quick Action Bar (Minimal 1-word buttons: New, Scan, Sort)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = {
                                if (activeViewTab == 0) showCreateDialog = true
                                else showAddCatalogItemDialog = true
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, cardBorder),
                            shadowElevation = if (isDarkTheme) 0.dp else 1.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = NawanoPrimary200.copy(alpha = if (isDarkTheme) 0.2f else 0.12f),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = NawanoPrimary200,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "New",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                            }
                        }

                        Surface(
                            onClick = { showLiveCameraScanner = true },
                            shape = RoundedCornerShape(16.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, cardBorder),
                            shadowElevation = if (isDarkTheme) 0.dp else 1.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFC084FC).copy(alpha = if (isDarkTheme) 0.2f else 0.12f),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = null,
                                            tint = Color(0xFFC084FC),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Scan",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                            }
                        }

                        Surface(
                            onClick = { isSortAscending = !isSortAscending },
                            shape = RoundedCornerShape(16.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, cardBorder),
                            shadowElevation = if (isDarkTheme) 0.dp else 1.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF10B981).copy(alpha = if (isDarkTheme) 0.2f else 0.12f),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.FilterList,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (isSortAscending) "Price" else "Recent",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                            }
                        }
                    }
                }

                // If in Orders View: Status Filter Pills
                if (activeViewTab == 0) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            filterTabs.forEachIndexed { index, name ->
                                val isSelected = selectedFilterIndex == index
                                Surface(
                                    onClick = { selectedFilterIndex = index },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) NawanoPrimary200 else cardBg,
                                    border = if (isSelected) null else BorderStroke(1.dp, cardBorder),
                                    shadowElevation = if (isSelected || isDarkTheme) 0.dp else 0.5.dp
                                ) {
                                    Text(
                                        text = name,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else textSecondary,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Orders List
                    if (filteredOrders.isEmpty()) {
                        item {
                            EmptyZeroState(
                                title = "No Orders",
                                subtitle = "Tap '+ New' to create an order.",
                                icon = Icons.Default.LocalShipping,
                                isDarkTheme = isDarkTheme
                            )
                        }
                    } else {
                        items(filteredOrders, key = { it.id }) { order ->
                            SupplierDashboardCard(
                                order = order,
                                isDarkTheme = isDarkTheme,
                                onAccept = {
                                    onConfirmOrder(order.id, null)
                                    Toast.makeText(context, "Order ${order.orderNumber} Accepted!", Toast.LENGTH_SHORT).show()
                                },
                                onSend = {
                                    orderToDispatch = order
                                },
                                onEdit = { orderToEdit = order },
                                onDelete = { orderToDelete = order },
                                onView = {
                                    orderToViewDetails = order
                                    scope.launch {
                                        detailedOrderWithItems = onLoadOrderDetails(order.id)
                                    }
                                }
                            )
                        }
                    }
                } else {
                    // Catalog (Items) View
                    if (filteredCatalogProducts.isEmpty()) {
                        item {
                            EmptyZeroState(
                                title = "Catalog Empty",
                                subtitle = "Tap '+ New' to add items you supply.",
                                icon = Icons.Default.Category,
                                isDarkTheme = isDarkTheme
                            )
                        }
                    } else {
                        items(filteredCatalogProducts, key = { it.id }) { product ->
                            SupplierCatalogProductCard(
                                product = product,
                                isDarkTheme = isDarkTheme,
                                onEdit = { catalogItemToEdit = product },
                                onDelete = {
                                    onDeleteProduct(product)
                                    Toast.makeText(context, "Removed ${product.name}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }

        // Floating Bottom Navigation Bar (Matches Middleman InventoryBottomBar design)
        SupplierBottomBar(
            activeViewTab = activeViewTab,
            onTabSelected = { activeViewTab = it },
            onAddClicked = {
                if (activeViewTab == 0) showCreateDialog = true
                else showAddCatalogItemDialog = true
            },
            onLogout = onLogout,
            isDarkTheme = isDarkTheme,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }

    // Modal 1: Dispatch Outbound Shipment Dialog with Carrier & Tracking Entry
    orderToDispatch?.let { order ->
        DispatchOutboundDialog(
            order = order,
            isDarkTheme = isDarkTheme,
            onDismiss = { orderToDispatch = null },
            onConfirmDispatch = { tracking, carrier, eta, weight, note ->
                onMarkDelivered(order.id, tracking, carrier, eta, weight, note)
                orderToDispatch = null
                Toast.makeText(context, "Order ${order.orderNumber} Dispatched!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal 2: Detailed Order Breakdown & Packing Slip Sheet (Just like Middleman Detail)
    orderToViewDetails?.let { order ->
        SupplierOrderDetailSheet(
            order = order,
            orderWithItems = detailedOrderWithItems,
            onDismiss = {
                orderToViewDetails = null
                detailedOrderWithItems = null
            },
            onSend = {
                orderToViewDetails = null
                orderToDispatch = order
            },
            onAccept = {
                onConfirmOrder(order.id, null)
                orderToViewDetails = null
                Toast.makeText(context, "Order ${order.orderNumber} Accepted!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal 3: Add Item to Supplier Catalog
    if (showAddCatalogItemDialog) {
        AddCatalogProductDialog(
            onDismiss = { showAddCatalogItemDialog = false },
            onSave = { product ->
                onAddProduct(product)
                showAddCatalogItemDialog = false
                Toast.makeText(context, "Added ${product.name} to Catalog!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal 4: Edit Catalog Item
    catalogItemToEdit?.let { product ->
        EditCatalogProductDialog(
            product = product,
            onDismiss = { catalogItemToEdit = null },
            onSave = { updated ->
                onUpdateProduct(updated)
                catalogItemToEdit = null
                Toast.makeText(context, "Updated ${updated.name}!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal 5: Create New Order
    if (showCreateDialog) {
        CreateOrderDialog(
            onDismiss = { showCreateDialog = false },
            onSave = { title, qty, price, estArrival ->
                onCreateOrder(title, qty, price, estArrival)
                showCreateDialog = false
                Toast.makeText(context, "Order Created!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal 6: Edit Order
    orderToEdit?.let { order ->
        EditOrderDialog(
            order = order,
            onDismiss = { orderToEdit = null },
            onSave = { newTitle, newQty, newPrice ->
                onUpdateOrder(order.id, newTitle, newQty, newPrice)
                orderToEdit = null
                Toast.makeText(context, "Order Updated!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal 7: Delete Order Confirmation
    orderToDelete?.let { order ->
        AlertDialog(
            containerColor = cardBg,
            onDismissRequest = { orderToDelete = null },
            title = {
                Text(
                    text = "Delete ${order.orderNumber}?",
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove this order from the system?",
                    color = textSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteOrder(order.id)
                        orderToDelete = null
                        Toast.makeText(context, "Order ${order.orderNumber} deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { orderToDelete = null }) {
                    Text("Cancel", color = textSecondary)
                }
            }
        )
    }
}

@Composable
private fun EmptyZeroState(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isDarkTheme: Boolean = false
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isDarkTheme) 0.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NawanoPrimary200.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = textSecondary
            )
        }
    }
}

@Composable
private fun SupplierDashboardCard(
    order: PurchaseOrderEntity,
    isDarkTheme: Boolean = false,
    onAccept: () -> Unit,
    onSend: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onView: () -> Unit
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight
    val dividerColor = if (isDarkTheme) NawanoDarkOutline.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
    val deleteBtnText = if (isDarkTheme) Color(0xFFF87171) else Color(0xFFDC2626)

    val (statusLabel, statusBg, statusFg) = if (isDarkTheme) {
        when (order.status) {
            PurchaseOrderStatus.PENDING -> Triple("Pending", Color(0xFFF59E0B).copy(alpha = 0.15f), Color(0xFFFBBF24))
            PurchaseOrderStatus.CONFIRMED -> Triple("Confirmed", Color(0xFF6366F1).copy(alpha = 0.15f), Color(0xFFC084FC))
            PurchaseOrderStatus.DELIVERED -> Triple("In Delivery", Color(0xFF6366F1).copy(alpha = 0.15f), Color(0xFFC084FC))
            PurchaseOrderStatus.RECEIVED -> Triple("Completed", Color(0xFF10B981).copy(alpha = 0.15f), Color(0xFF34D399))
            PurchaseOrderStatus.REJECTED -> Triple("Rejected", Color(0xFFEF4444).copy(alpha = 0.15f), Color(0xFFF87171))
            PurchaseOrderStatus.CANCELLED -> Triple("Cancelled", Color(0xFF6B7280).copy(alpha = 0.15f), Color(0xFF9CA3AF))
        }
    } else {
        when (order.status) {
            PurchaseOrderStatus.PENDING -> Triple("Pending", Color(0xFFFEF3C7), Color(0xFFD97706))
            PurchaseOrderStatus.CONFIRMED -> Triple("Confirmed", Color(0xFFEDE9FE), Color(0xFF6D28D9))
            PurchaseOrderStatus.DELIVERED -> Triple("In Delivery", Color(0xFFE0E7FF), Color(0xFF4338CA))
            PurchaseOrderStatus.RECEIVED -> Triple("Completed", Color(0xFFD1FAE5), Color(0xFF047857))
            PurchaseOrderStatus.REJECTED -> Triple("Rejected", Color(0xFFFEE2E2), Color(0xFFDC2626))
            PurchaseOrderStatus.CANCELLED -> Triple("Cancelled", Color(0xFFF3F4F6), Color(0xFF4B5563))
        }
    }

    Surface(
        onClick = onView,
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isDarkTheme) 0.dp else 1.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Status Badge on left + Action Button on right (Exact Middleman design)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBg
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when (order.status) {
                        PurchaseOrderStatus.PENDING -> {
                            Surface(
                                onClick = onAccept,
                                shape = RoundedCornerShape(14.dp),
                                color = NawanoPrimary200
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Accept",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        PurchaseOrderStatus.CONFIRMED -> {
                            Surface(
                                onClick = onSend,
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF4338CA)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalShipping,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Dispatch",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        else -> {
                            Surface(
                                onClick = onView,
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDarkTheme) Color(0xFF2A2E63) else Color(0xFFEEF2FF)
                            ) {
                                Text(
                                    text = "View",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) Color.White else Color(0xFF4338CA),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = deleteBtnText,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title & Arrival (Identical to Middleman layout)
            Text(
                text = "${order.orderNumber} - ${order.title.ifBlank { "Inbound Goods Request" }}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "Est: ${order.estArrival.ifBlank { "Today" }} • ${order.picSender.ifBlank { "ColdFreight Express" }}",
                style = MaterialTheme.typography.labelSmall,
                color = textSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = dividerColor)
            Spacer(modifier = Modifier.height(8.dp))

            // Footer: Terms/Tracking on Left + Amount & Qty on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (order.trackingNumber.isNotBlank()) "Track: ${order.trackingNumber} • Net 30" else "Wholesale PO • Net 30",
                    style = MaterialTheme.typography.labelSmall,
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", order.totalAmount)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF047857)
                    )
                    Text(
                        text = "Total: +${order.totalQuantity} units",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = NawanoPrimary200
                    )
                }
            }
        }
    }
}

@Composable
private fun SupplierCatalogProductCard(
    product: ProductEntity,
    isDarkTheme: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight
    val dividerColor = if (isDarkTheme) NawanoDarkOutline.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
    val badgeBg = if (isDarkTheme) NawanoPrimary200.copy(alpha = 0.15f) else Color(0xFFEDE9FE)
    val badgeFg = if (isDarkTheme) NawanoPrimary200 else Color(0xFF6D28D9)
    val deleteBtnText = if (isDarkTheme) Color(0xFFF87171) else Color(0xFFDC2626)

    Surface(
        onClick = onEdit,
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isDarkTheme) 0.dp else 1.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Category Badge on left + Edit/Del on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = product.category.ifBlank { "General" },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = onEdit,
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDarkTheme) Color(0xFF2A2E63) else Color(0xFFEEF2FF)
                    ) {
                        Text(
                            text = "Edit",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color.White else Color(0xFF4338CA),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = deleteBtnText,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // SKU & Title
            Text(
                text = "${product.sku} - ${product.name}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "Wholesale Unit: ${product.unit} • Active Catalog",
                style = MaterialTheme.typography.labelSmall,
                color = textSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = dividerColor)
            Spacer(modifier = Modifier.height(8.dp))

            // Footer: Supply Price on left + Quantity on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Supply Price: $${String.format(Locale.US, "%.2f", product.costPrice)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF047857)
                )

                Text(
                    text = "Stock: ${product.quantity} ${product.unit}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = NawanoPrimary200
                )
            }
        }
    }
}

/**
 * Modern Floating Bottom Navigation Dock for Supplier Orders & Catalog (Identical to Middleman).
 */
@Composable
private fun SupplierBottomBar(
    activeViewTab: Int,
    onTabSelected: (Int) -> Unit,
    onAddClicked: () -> Unit,
    onLogout: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(top = 18.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = if (isDarkTheme) NawanoDark200 else Color.White,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, if (isDarkTheme) NawanoDarkOutline else NawanoPrimary200.copy(alpha = 0.20f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 0: Orders
                FloatingNavItem(
                    title = "Orders",
                    icon = if (activeViewTab == 0) Icons.Default.Inventory2 else Icons.Outlined.Inventory2,
                    isSelected = activeViewTab == 0,
                    onClick = { onTabSelected(0) }
                )

                // Tab 1: Catalog
                FloatingNavItem(
                    title = "Catalog",
                    icon = if (activeViewTab == 1) Icons.Default.Category else Icons.Outlined.Category,
                    isSelected = activeViewTab == 1,
                    onClick = { onTabSelected(1) }
                )

                // Center placeholder space for the elevated pop-up action button
                Spacer(modifier = Modifier.width(56.dp))

                // Tab 2: Scan / Add
                FloatingNavItem(
                    title = "New",
                    icon = Icons.Outlined.AddBox,
                    isSelected = false,
                    onClick = onAddClicked
                )

                // Tab 3: Exit
                FloatingNavItem(
                    title = "Exit",
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    isSelected = false,
                    onClick = onLogout
                )
            }
        }

        // Center Popped-Up Floating Add Button (Exact same design as Middleman FloatingScanButton)
        Surface(
            onClick = onAddClicked,
            shape = CircleShape,
            color = Color.Transparent,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(4.dp, if (isDarkTheme) NawanoDark400 else MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-18).dp)
                .size(58.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(NawanoGradientStart, NawanoGradientEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Order or Product",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun DispatchOutboundDialog(
    order: PurchaseOrderEntity,
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit,
    onConfirmDispatch: (tracking: String, carrier: String, eta: String, weight: String, note: String) -> Unit
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight
    val fieldBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFCBD5E1)

    val defaultTracking = remember(order.id) {
        if (order.trackingNumber.isNotBlank()) order.trackingNumber
        else "TRK-${(1000..9999).random()}"
    }
    var trackingNumber by remember { mutableStateOf(defaultTracking) }
    var carrier by remember { mutableStateOf(if (order.picSender.isNotBlank()) order.picSender else "ColdFreight Express") }
    var etaWindow by remember { mutableStateOf(if (order.estArrival.isNotBlank()) order.estArrival else "Today, 02:00 - 04:30 PM") }
    var weight by remember { mutableStateOf(if (order.totalWeight.isNotBlank()) order.totalWeight else "14.5 KG") }
    var dispatchNote by remember { mutableStateOf("Refrigerated freight. Temperature logged.") }

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
                    value = carrier,
                    onValueChange = { carrier = it },
                    label = { Text("Carrier / Freight Line", color = textSecondary) },
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
                        value = trackingNumber,
                        onValueChange = { trackingNumber = it },
                        label = { Text("Tracking Code", color = textSecondary) },
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
                        label = { Text("Weight", color = textSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedBorderColor = NawanoPrimary200,
                            unfocusedBorderColor = fieldBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = etaWindow,
                    onValueChange = { etaWindow = it },
                    label = { Text("Estimated Arrival", color = textSecondary) },
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
                    label = { Text("Packing & Dispatch Note", color = textSecondary) },
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
                Text("Dispatch", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textSecondary)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupplierOrderDetailSheet(
    order: PurchaseOrderEntity,
    orderWithItems: OrderWithItems?,
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit,
    onSend: () -> Unit,
    onAccept: () -> Unit
) {
    val context = LocalContext.current
    val items = orderWithItems?.items ?: emptyList()

    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight
    val dividerColor = if (isDarkTheme) NawanoDarkOutline.copy(alpha = 0.6f) else Color(0xFFF1F5F9)
    val itemCardBg = if (isDarkTheme) NawanoDark400 else Color(0xFFF8FAFC)
    val itemCardBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
    val itemCheckedBg = if (isDarkTheme) Color(0xFF0F2E22) else Color(0xFFD1FAE5)

    // Interactive warehouse packing checklist
    val packedItemsMap = remember { mutableStateMapOf<String, Boolean>() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = cardBg,
        dragHandle = { BottomSheetDefaults.DragHandle(color = if (isDarkTheme) NawanoDarkOutline else Color(0xFFCBD5E1)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header: PO Number + Copy + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = order.orderNumber,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Copy",
                        tint = NawanoPrimary200,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("PO Number", order.orderNumber))
                                Toast.makeText(context, "Copied ${order.orderNumber}", Toast.LENGTH_SHORT).show()
                            }
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDarkTheme) NawanoPrimary200.copy(alpha = 0.2f) else Color(0xFFEDE9FE)
                ) {
                    Text(
                        text = order.status.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) NawanoPrimary200 else Color(0xFF6D28D9),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = order.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary
            )

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = dividerColor)
            Spacer(Modifier.height(14.dp))

            // Logistics Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Carrier & Route", fontSize = 11.sp, color = textSecondary)
                    Text(
                        text = "${order.picSender.ifBlank { "Apex Logistics" }} -> Nawano HQ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Tracking Code", fontSize = 11.sp, color = textSecondary)
                    Text(
                        text = order.trackingNumber.ifBlank { "Pending Dispatch" },
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) Color(0xFFC084FC) else Color(0xFF6366F1)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Packing Slip Line Items Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Packing Checklist (${if (items.isEmpty()) 1 else items.size} items)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Text(
                    text = "Tap to verify",
                    fontSize = 11.sp,
                    color = textSecondary
                )
            }

            Spacer(Modifier.height(8.dp))

            // Line items list
            if (items.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = itemCardBg,
                    border = BorderStroke(1.dp, itemCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(order.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            Text("${order.totalQuantity} Units", fontSize = 11.sp, color = textSecondary)
                        }
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", order.totalAmount)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { item ->
                        val isChecked = packedItemsMap[item.id] ?: false
                        Surface(
                            onClick = { packedItemsMap[item.id] = !isChecked },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isChecked) itemCheckedBg else itemCardBg,
                            border = BorderStroke(1.dp, if (isChecked) Color(0xFF10B981) else itemCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (isChecked) Color(0xFF10B981) else textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(item.productName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                        Text("SKU: ${item.sku} • ${item.quantity} Qty @ $${item.unitCost}", fontSize = 11.sp, color = textSecondary)
                                    }
                                }
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", item.quantity * item.unitCost)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = dividerColor)
            Spacer(Modifier.height(14.dp))

            // Financial Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total Invoice Value", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                Text(
                    text = "$${String.format(Locale.US, "%.2f", order.totalAmount)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF10B981)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (order.status == PurchaseOrderStatus.PENDING) {
                    Button(
                        onClick = onAccept,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Accept Order", fontWeight = FontWeight.Bold)
                    }
                } else if (order.status == PurchaseOrderStatus.CONFIRMED) {
                    Button(
                        onClick = onSend,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4338CA)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Dispatch Shipment", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isDarkTheme) NawanoDarkOutline else Color(0xFFCBD5E1)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Close", color = textPrimary)
                }
            }
        }
    }
}

@Composable
private fun AddCatalogProductDialog(
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight
    val fieldBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFCBD5E1)

    var name by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("SKU-${(1000..9999).random()}") }
    var category by remember { mutableStateOf("Seafood") }
    var costPrice by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("Kg") }

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

@Composable
private fun EditCatalogProductDialog(
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

@Composable
private fun CreateOrderDialog(
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (title: String, quantity: Int, price: Double, estArrival: String) -> Unit
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight
    val fieldBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFCBD5E1)

    var title by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var estArrival by remember { mutableStateOf("Today") }

    AlertDialog(
        containerColor = cardBg,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Order",
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Item Name (e.g. Scallops)", color = textSecondary) },
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
                        value = qty,
                        onValueChange = { qty = it },
                        placeholder = { Text("Qty", color = textSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedBorderColor = NawanoPrimary200,
                            unfocusedBorderColor = fieldBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        placeholder = { Text("Price ($)", color = textSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedBorderColor = NawanoPrimary200,
                            unfocusedBorderColor = fieldBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = estArrival,
                    onValueChange = { estArrival = it },
                    placeholder = { Text("Est Arrival (e.g. 02.00 PM)", color = textSecondary) },
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
                    val q = qty.toIntOrNull() ?: 1
                    val p = price.toDoubleOrNull() ?: 0.0
                    onSave(title.ifBlank { "Goods Inbound" }, q, p, estArrival)
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

@Composable
private fun EditOrderDialog(
    order: PurchaseOrderEntity,
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (title: String, quantity: Int, price: Double) -> Unit
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight
    val fieldBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFCBD5E1)

    var title by remember { mutableStateOf(order.title) }
    var qty by remember { mutableStateOf(order.totalQuantity.toString()) }
    var price by remember { mutableStateOf(order.totalAmount.toString()) }

    AlertDialog(
        containerColor = cardBg,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit ${order.orderNumber}",
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Item Title", color = textSecondary) },
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
                        value = qty,
                        onValueChange = { qty = it },
                        label = { Text("Quantity", color = textSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedBorderColor = NawanoPrimary200,
                            unfocusedBorderColor = fieldBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Total ($)", color = textSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                    val q = qty.toIntOrNull() ?: order.totalQuantity
                    val p = price.toDoubleOrNull() ?: order.totalAmount
                    onSave(title, q, p)
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
