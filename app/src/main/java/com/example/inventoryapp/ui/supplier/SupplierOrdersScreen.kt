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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
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

    // 0 = Orders view, 1 = Catalog (Items) view, 2 = Stats view, 3 = Settings view
    var activeViewTab by remember { mutableIntStateOf(0) }
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val filterTabs = listOf("All", "Pending", "Transit", "Done")

    // Modals, Sheets & Drawer
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showNotificationSheet by remember { mutableStateOf(false) }
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SupplierDrawerSheet(
                userName = user.fullName.replace(" (Supplier)", ""),
                initials = initials,
                activeViewTab = activeViewTab,
                onTabSelected = { tab ->
                    activeViewTab = tab
                    scope.launch { drawerState.close() }
                },
                ordersCount = orders.size,
                catalogCount = products.size,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onCloseDrawer = { scope.launch { drawerState.close() } },
                onLogout = onLogout
            )
        }
    ) {
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
                // Header Top Bar (Menu, Profile, Dark/Light Mode, Notifications)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Drawer Menu Button (Hamburger)
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Navigation Menu",
                                tint = textPrimary
                            )
                        }

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
                                text = when (activeViewTab) {
                                    0 -> "Supplier Portal • ${orders.size} Orders"
                                    1 -> "Wholesale Catalog • ${products.size} SKUs"
                                    2 -> "Analytics & Performance"
                                    else -> "Supplier Settings"
                                },
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
                        // Dark / Light Mode Toggle Button
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

                        // Notification Bell with Badge
                        val pendingCount = remember(orders) { orders.count { it.status == PurchaseOrderStatus.PENDING } }
                        IconButton(
                            onClick = { showNotificationSheet = true },
                            modifier = Modifier.size(38.dp)
                        ) {
                            BadgedBox(
                                badge = {
                                    if (pendingCount > 0) {
                                        Badge(
                                            containerColor = Color(0xFFEF4444),
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                text = pendingCount.toString(),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (pendingCount > 0) Icons.Default.Notifications else Icons.Outlined.Notifications,
                                    contentDescription = "Notifications",
                                    tint = textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
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
                if (activeViewTab == 0 || activeViewTab == 1) {
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
                }

                // View 0: Orders List
                if (activeViewTab == 0) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Status Filter Pills
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

                            // Underneath Filter: Order counter & Sort Button (Moved to under)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${filteredOrders.size} Orders",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )

                                Surface(
                                    onClick = { isSortAscending = !isSortAscending },
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

                    // Orders List
                    if (filteredOrders.isEmpty()) {
                        item {
                            EmptyZeroState(
                                title = "No Orders",
                                subtitle = "Tap '+' to create an order.",
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
                } else if (activeViewTab == 1) {
                    // View 1: Catalog (Items) View
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${filteredCatalogProducts.size} Products",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )

                            Surface(
                                onClick = { isSortAscending = !isSortAscending },
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

                    if (filteredCatalogProducts.isEmpty()) {
                        item {
                            EmptyZeroState(
                                title = "Catalog Empty",
                                subtitle = "Tap '+' to add items you supply.",
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
                } else if (activeViewTab == 2) {
                    // View 2: Analytics & Stats View
                    item {
                        SupplierStatsView(
                            orders = orders,
                            products = products,
                            isDarkTheme = isDarkTheme
                        )
                    }
                } else if (activeViewTab == 3) {
                    // View 3: Supplier Settings View
                    item {
                        SupplierSettingsView(
                            user = user,
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = onToggleTheme,
                            onLogout = onLogout
                        )
                    }
                }
            }
        }

        // Floating Bottom Navigation Bar (Matches Middleman InventoryBottomBar design)
        SupplierBottomBar(
            activeViewTab = activeViewTab,
            onTabSelected = { activeViewTab = it },
            onAddClicked = {
                if (activeViewTab == 1) showAddCatalogItemDialog = true
                else showCreateDialog = true
            },
            isDarkTheme = isDarkTheme,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

    // Supplier Notifications Modal Bottom Sheet
    if (showNotificationSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNotificationSheet = false },
            containerColor = if (isDarkTheme) NawanoDark200 else Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = NawanoPrimary200,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Supplier Notifications",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }
                    TextButton(onClick = { showNotificationSheet = false }) {
                        Text("Close", color = NawanoPrimary200, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(14.dp))

                NotificationItemRow(
                    title = "Shipment In Delivery",
                    subtitle = "PO-1111-1183 (TRK-8EABAD26) has been dispatched.",
                    time = "10m ago",
                    icon = Icons.Default.LocalShipping,
                    isDarkTheme = isDarkTheme
                )

                Spacer(Modifier.height(10.dp))

                NotificationItemRow(
                    title = "Wholesale Catalog Synced",
                    subtitle = "Catalog items and prices synchronized with Middleman HQ.",
                    time = "1h ago",
                    icon = Icons.Default.Sync,
                    isDarkTheme = isDarkTheme
                )

                Spacer(Modifier.height(10.dp))

                NotificationItemRow(
                    title = "Settlement Period Active",
                    subtitle = "Net 30 terms in good standing for Pacific Seafood Supply.",
                    time = "Today",
                    icon = Icons.Default.AccountBalanceWallet,
                    isDarkTheme = isDarkTheme
                )

                Spacer(Modifier.height(28.dp))
            }
        }
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
 * Tabs: Orders (0), Catalog (1), Stats (2), Settings (3) + Elevated Pop-Up '+' Action Button.
 */
@Composable
private fun SupplierBottomBar(
    activeViewTab: Int,
    onTabSelected: (Int) -> Unit,
    onAddClicked: () -> Unit,
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

                // Tab 2: Stats (Replaces "New")
                FloatingNavItem(
                    title = "Stats",
                    icon = if (activeViewTab == 2) Icons.Default.BarChart else Icons.Outlined.BarChart,
                    isSelected = activeViewTab == 2,
                    onClick = { onTabSelected(2) }
                )

                // Tab 3: Settings (Replaces "Exit")
                FloatingNavItem(
                    title = "Settings",
                    icon = if (activeViewTab == 3) Icons.Default.Settings else Icons.Outlined.Settings,
                    isSelected = activeViewTab == 3,
                    onClick = { onTabSelected(3) }
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

/**
 * Navigation Drawer Sheet for Supplier (Matches Middleman InventoryDrawerSheet).
 */
@Composable
private fun SupplierDrawerSheet(
    userName: String,
    initials: String,
    activeViewTab: Int,
    onTabSelected: (Int) -> Unit,
    ordersCount: Int,
    catalogCount: Int,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onCloseDrawer: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    ModalDrawerSheet(
        modifier = modifier
            .width(310.dp)
            .fillMaxHeight(),
        drawerContainerColor = if (isDarkTheme) NawanoDark200 else Color.White,
        drawerTonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp, horizontal = 16.dp)
        ) {
            // Profile & Branding Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDarkTheme) NawanoDark200 else Color(0xFFF3E8FF),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, NawanoPrimary200.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(NawanoGradientStart, NawanoGradientEnd)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }

                        IconButton(
                            onClick = onCloseDrawer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Drawer",
                                tint = textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "Verified Supplier • Nawano Partner",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "PORTAL VIEWS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Item 1: Orders
            SupplierDrawerItem(
                label = "Orders & Dispatches",
                icon = Icons.Default.Inventory2,
                badge = "$ordersCount",
                isSelected = activeViewTab == 0,
                isDarkTheme = isDarkTheme,
                onClick = { onTabSelected(0) }
            )

            // Item 2: Catalog
            SupplierDrawerItem(
                label = "Wholesale Catalog",
                icon = Icons.Default.Category,
                badge = "$catalogCount",
                isSelected = activeViewTab == 1,
                isDarkTheme = isDarkTheme,
                onClick = { onTabSelected(1) }
            )

            // Item 3: Stats
            SupplierDrawerItem(
                label = "Analytics & Performance",
                icon = Icons.Default.BarChart,
                badge = "KPIs",
                isSelected = activeViewTab == 2,
                isDarkTheme = isDarkTheme,
                onClick = { onTabSelected(2) }
            )

            // Item 4: Settings
            SupplierDrawerItem(
                label = "Supplier Settings",
                icon = Icons.Default.Settings,
                isSelected = activeViewTab == 3,
                isDarkTheme = isDarkTheme,
                onClick = { onTabSelected(3) }
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0))
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "PREFERENCES & SYSTEM",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Switch to Dark / Light Mode
            Surface(
                onClick = onToggleTheme,
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = null,
                        tint = if (isDarkTheme) Color(0xFFFBBF24) else NawanoPrimary200,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(24.dp))

            // Log Out Button
            Surface(
                onClick = onLogout,
                shape = RoundedCornerShape(12.dp),
                color = if (isDarkTheme) Color(0xFF2D1515) else Color(0xFFFEF2F2),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Log Out of Supplier",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                }
            }
        }
    }
}

@Composable
private fun SupplierDrawerItem(
    label: String,
    icon: ImageVector,
    badge: String? = null,
    isSelected: Boolean,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val selectedBg = if (isDarkTheme) NawanoPrimary200.copy(alpha = 0.25f) else Color(0xFFEEF2FF)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) selectedBg else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, NawanoPrimary200.copy(alpha = 0.4f)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) NawanoPrimary200 else (if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) NawanoPrimary200 else textPrimary,
                modifier = Modifier.weight(1f)
            )
            badge?.let {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) NawanoPrimary200 else (if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0))
                ) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else (if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationItemRow(
    title: String,
    subtitle: String,
    time: String,
    icon: ImageVector,
    isDarkTheme: Boolean
) {
    val cardBg = if (isDarkTheme) NawanoDarkCard else Color(0xFFF8FAFC)
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = NawanoPrimary200.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = NawanoPrimary200,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelSmall,
                        color = textSecondary
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondary
                )
            }
        }
    }
}

@Composable
private fun SupplierStatsView(
    orders: List<PurchaseOrderEntity>,
    products: List<ProductEntity>,
    isDarkTheme: Boolean
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    val totalValue = orders.sumOf { it.totalAmount }
    val totalQty = orders.sumOf { it.totalQuantity }
    val pendingCount = orders.count { it.status == PurchaseOrderStatus.PENDING }
    val inTransitCount = orders.count { it.status == PurchaseOrderStatus.DELIVERED }
    val completedCount = orders.count { it.status == PurchaseOrderStatus.RECEIVED }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Performance Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF6366F1), NawanoPrimary200, Color(0xFF9061F9))
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Supplier Performance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Tier: Gold Partner",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Fulfillment Rate", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                            Text("98.4%", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("On-Time Dispatch", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                            Text("96.8%", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }
            }
        }

        // Metrics Grid (Revenue & Total Qty)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Supply Value", style = MaterialTheme.typography.labelSmall, color = textSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$${String.format(Locale.US, "%,.2f", totalValue)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF047857)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text("${orders.size} Total Orders", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Units Supplied", style = MaterialTheme.typography.labelSmall, color = textSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$totalQty Units",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = NawanoPrimary200
                    )
                    Spacer(Modifier.height(2.dp))
                    Text("${products.size} Catalog SKUs", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                }
            }
        }

        // Order Status Distribution Breakdown
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Order Distribution Pipeline",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Spacer(Modifier.height(12.dp))

                StatusProgressRow(
                    label = "In Delivery",
                    count = inTransitCount,
                    total = orders.size.coerceAtLeast(1),
                    color = Color(0xFF6366F1),
                    isDarkTheme = isDarkTheme
                )
                Spacer(Modifier.height(8.dp))

                StatusProgressRow(
                    label = "Pending Confirmation",
                    count = pendingCount,
                    total = orders.size.coerceAtLeast(1),
                    color = Color(0xFFF59E0B),
                    isDarkTheme = isDarkTheme
                )
                Spacer(Modifier.height(8.dp))

                StatusProgressRow(
                    label = "Completed & Received",
                    count = completedCount,
                    total = orders.size.coerceAtLeast(1),
                    color = Color(0xFF10B981),
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }
}

@Composable
private fun StatusProgressRow(
    label: String,
    count: Int,
    total: Int,
    color: Color,
    isDarkTheme: Boolean
) {
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight
    val ratio = (count.toFloat() / total.toFloat()).coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = textPrimary)
            Text("$count orders", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = textSecondary)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}

@Composable
private fun SupplierSettingsView(
    user: UserEntity,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Supplier Profile Information Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Supplier Profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(NawanoGradientStart, NawanoGradientEnd)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PS",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = user.fullName.replace(" (Supplier)", ""),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = user.email.ifBlank { "supplier@nawano.com" },
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary
                        )
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Verified Active Partner",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // App & Portal Preferences
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Portal Preferences",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Spacer(Modifier.height(12.dp))

                // Dark Theme Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleTheme() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Dark Mode", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = textPrimary)
                        Text(if (isDarkTheme) "Enabled (High Contrast Dark)" else "Disabled (Clean Light)", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { onToggleTheme() }
                    )
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = cardBorder)
                Spacer(Modifier.height(10.dp))

                // Auto Delivery Sync
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Carrier Tracking Sync", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = textPrimary)
                        Text("Automatically update tracking for Middleman", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                    }
                    Switch(
                        checked = true,
                        onCheckedChange = {}
                    )
                }
            }
        }

        // Log Out Button
        Surface(
            onClick = onLogout,
            shape = RoundedCornerShape(16.dp),
            color = if (isDarkTheme) Color(0xFF2D1515) else Color(0xFFFEF2F2),
            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Log Out of Supplier Portal",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444)
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
