package com.example.inventoryapp.ui.supplier

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderStatus
import com.example.inventoryapp.data.local.entity.UserEntity
import com.example.inventoryapp.data.repository.OrderWithItems
import com.example.inventoryapp.ui.scanner.BarcodeScannerScreen
import com.example.inventoryapp.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

enum class SupplierScannerTarget {
    NONE,
    GENERAL_SCAN,
    CATALOG_SKU_SCAN,
    DISPATCH_TRACKING_SCAN,
    PACKING_CHECKLIST_SCAN,
    CREATE_ORDER_SKU_SCAN
}

/**
 * Supplier Portal Main Coordinator Screen.
 * Orchestrates Orders, Catalog, Freight Shipments, Vendor Analytics, and Settings.
 */
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

    // Active View Tab:
    // 0: Orders, 1: Wholesale Catalog, 2: Freight Shipments, 3: Analytics, 4: Settings
    var activeViewTab by remember { mutableIntStateOf(0) }
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val filterTabs = listOf("All", "Pending", "Transit", "Done")

    // Drawer, Modals & Dialogs State
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showNotificationSheet by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var orderToEdit by remember { mutableStateOf<PurchaseOrderEntity?>(null) }
    var orderToDelete by remember { mutableStateOf<PurchaseOrderEntity?>(null) }
    var orderToDispatch by remember { mutableStateOf<PurchaseOrderEntity?>(null) }
    var orderToViewDetails by remember { mutableStateOf<PurchaseOrderEntity?>(null) }
    var detailedOrderWithItems by remember { mutableStateOf<OrderWithItems?>(null) }

    // Live Scanner & Search State
    var scannerTarget by remember { mutableStateOf(SupplierScannerTarget.NONE) }
    var scannedSkuForCatalog by remember { mutableStateOf("") }
    var scannedTrackingForDispatch by remember { mutableStateOf("") }
    var scannedSkuForPacking by remember { mutableStateOf<String?>(null) }
    var scannedSkuForNewOrder by remember { mutableStateOf<String?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var isSortAscending by remember { mutableStateOf(false) }

    // Catalog Modals
    var showAddCatalogItemDialog by remember { mutableStateOf(false) }
    var catalogItemToEdit by remember { mutableStateOf<ProductEntity?>(null) }

    // Metrics calculations
    val totalOrders = orders.size
    val totalSkus = remember(orders) { orders.map { it.orderNumber }.distinct().size }
    val totalQty = remember(orders) { orders.sumOf { it.totalQuantity } }
    val totalValue = remember(orders) { orders.sumOf { it.totalAmount } }
    val inTransitCount = remember(orders) { orders.count { it.status == PurchaseOrderStatus.DELIVERED } }
    val pendingCount = remember(orders) { orders.count { it.status == PurchaseOrderStatus.PENDING } }

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

    val filteredCatalogProducts = remember(products, searchQuery, isSortAscending) {
        val list = if (searchQuery.isBlank()) products
        else products.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.sku.contains(searchQuery, ignoreCase = true)
        }
        if (isSortAscending) list.sortedBy { it.costPrice } else list.sortedByDescending { it.updatedAt }
    }

    val initials = remember(user.fullName) {
        val parts = user.fullName.trim().split(" ")
        if (parts.size >= 2) {
            "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
        } else {
            user.fullName.take(2).uppercase()
        }
    }

    // Camera Barcode Scanner View Overlay with Full Contextual Target Dispatch
    if (scannerTarget != SupplierScannerTarget.NONE) {
        BarcodeScannerScreen(
            onBarcodeScanned = { scanned ->
                val clean = scanned.trim()
                when (scannerTarget) {
                    SupplierScannerTarget.GENERAL_SCAN -> {
                        scannerTarget = SupplierScannerTarget.NONE
                        // Check if matches order number or tracking
                        val matchedOrder = orders.find {
                            it.orderNumber.contains(clean, ignoreCase = true) ||
                            (it.trackingNumber.isNotBlank() && it.trackingNumber.contains(clean, ignoreCase = true))
                        }
                        if (matchedOrder != null) {
                            orderToViewDetails = matchedOrder
                            scope.launch {
                                detailedOrderWithItems = onLoadOrderDetails(matchedOrder.id)
                            }
                            Toast.makeText(context, "Matched Order: ${matchedOrder.orderNumber}", Toast.LENGTH_SHORT).show()
                        } else {
                            // Check if matches catalog product
                            val matchedProd = products.find { it.sku.contains(clean, ignoreCase = true) }
                            if (matchedProd != null) {
                                catalogItemToEdit = matchedProd
                                Toast.makeText(context, "Matched Catalog Item: ${matchedProd.name}", Toast.LENGTH_SHORT).show()
                            } else {
                                // Neither order nor catalog -> offer to register into wholesale catalog
                                scannedSkuForCatalog = clean
                                showAddCatalogItemDialog = true
                                Toast.makeText(context, "SKU '$clean' scanned. Add to Wholesale Catalog?", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    SupplierScannerTarget.CATALOG_SKU_SCAN -> {
                        scannerTarget = SupplierScannerTarget.NONE
                        scannedSkuForCatalog = clean
                        showAddCatalogItemDialog = true
                        Toast.makeText(context, "Scanned SKU: $clean", Toast.LENGTH_SHORT).show()
                    }
                    SupplierScannerTarget.DISPATCH_TRACKING_SCAN -> {
                        scannerTarget = SupplierScannerTarget.NONE
                        scannedTrackingForDispatch = clean
                        Toast.makeText(context, "Waybill Scanned: $clean", Toast.LENGTH_SHORT).show()
                    }
                    SupplierScannerTarget.PACKING_CHECKLIST_SCAN -> {
                        scannerTarget = SupplierScannerTarget.NONE
                        scannedSkuForPacking = clean
                    }
                    SupplierScannerTarget.CREATE_ORDER_SKU_SCAN -> {
                        scannerTarget = SupplierScannerTarget.NONE
                        scannedSkuForNewOrder = clean
                        showCreateDialog = true
                    }
                    SupplierScannerTarget.NONE -> {}
                }
            },
            onClose = {
                when (scannerTarget) {
                    SupplierScannerTarget.CATALOG_SKU_SCAN -> showAddCatalogItemDialog = true
                    SupplierScannerTarget.CREATE_ORDER_SKU_SCAN -> showCreateDialog = true
                    else -> {}
                }
                scannerTarget = SupplierScannerTarget.NONE
            }
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
                shipmentsCount = inTransitCount,
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
            Column(modifier = Modifier.fillMaxSize()) {
                // Modular Top App Bar
                SupplierTopBar(
                    userName = user.fullName.replace(" (Supplier)", ""),
                    initials = initials,
                    activeViewTab = activeViewTab,
                    ordersCount = orders.size,
                    catalogCount = products.size,
                    shipmentsCount = inTransitCount,
                    pendingNotificationCount = pendingCount,
                    isDarkTheme = isDarkTheme,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onToggleTheme = onToggleTheme,
                    onOpenNotifications = { showNotificationSheet = true },
                    onOpenScanner = { scannerTarget = SupplierScannerTarget.GENERAL_SCAN }
                )

                // Main Content Switching by Tab
                when (activeViewTab) {
                    0 -> {
                        // TAB 0: Orders List View
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
                                                            imageVector = Icons.Default.Layers,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = "Wholesale Summary",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }

                                            Spacer(Modifier.height(18.dp))

                                            Row(modifier = Modifier.fillMaxWidth()) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Orders", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.75f))
                                                    Text("$totalOrders", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("SKUs", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.75f))
                                                    Text("$totalSkus", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Qty", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.75f))
                                                    Text("$totalQty", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                                Column(modifier = Modifier.weight(1.2f)) {
                                                    Text("Value", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.75f))
                                                    Text("$${String.format(Locale.US, "%,.0f", totalValue)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. Filter Tabs & Sort Controls
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
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

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // + New Order Button
                                            Surface(
                                                onClick = {
                                                    scannedSkuForNewOrder = null
                                                    showCreateDialog = true
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                color = NawanoPrimary200,
                                                shadowElevation = if (isDarkTheme) 0.dp else 1.dp
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = "New Order",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(
                                                        text = "New Order",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }
                                            }

                                            // Sort Button
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
                            }

                            // 3. Orders List Items
                            if (filteredOrders.isEmpty()) {
                                item {
                                    EmptyZeroState(
                                        title = "No Orders",
                                        subtitle = "Tap '+' to issue or create an order.",
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
                        }
                    }
                    1 -> {
                        // TAB 1: Dedicated Wholesale Catalog Tab
                        SupplierCatalogTab(
                            products = filteredCatalogProducts,
                            isDarkTheme = isDarkTheme,
                            isSortAscending = isSortAscending,
                            onToggleSort = { isSortAscending = !isSortAscending },
                            onAddProduct = {
                                scannedSkuForCatalog = ""
                                showAddCatalogItemDialog = true
                            },
                            onEditProduct = { catalogItemToEdit = it },
                            onDeleteProduct = {
                                onDeleteProduct(it)
                                Toast.makeText(context, "Removed ${it.name}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    2 -> {
                        // TAB 2: Vendor Analytics & Performance Screen (Stats)
                        SupplierAnalyticsScreen(
                            orders = orders,
                            products = products,
                            isDarkTheme = isDarkTheme
                        )
                    }
                    3 -> {
                        // TAB 3: Supplier Profile & Settings
                        SupplierSettingsScreen(
                            user = user,
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = onToggleTheme,
                            onLogout = onLogout
                        )
                    }
                }
            }

            // Floating Bottom Navigation Dock with Central QR/Barcode Scanner
            SupplierBottomBar(
                activeViewTab = activeViewTab,
                onTabSelected = { activeViewTab = it },
                onScanClicked = {
                    scannerTarget = SupplierScannerTarget.GENERAL_SCAN
                },
                isDarkTheme = isDarkTheme,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }

    // Modal 1: Supplier Notifications Sheet
    if (showNotificationSheet) {
        SupplierNotificationsSheet(
            orders = orders,
            isDarkTheme = isDarkTheme,
            onDismiss = { showNotificationSheet = false }
        )
    }

    // Modal 2: Detailed Order Breakdown & Packing Slip Sheet
    orderToViewDetails?.let { order ->
        SupplierOrderDetailSheet(
            order = order,
            orderWithItems = detailedOrderWithItems,
            isDarkTheme = isDarkTheme,
            packedItemSku = scannedSkuForPacking,
            onDismiss = {
                orderToViewDetails = null
                detailedOrderWithItems = null
                scannedSkuForPacking = null
            },
            onSend = {
                orderToViewDetails = null
                orderToDispatch = order
            },
            onAccept = {
                onConfirmOrder(order.id, null)
                Toast.makeText(context, "Order ${order.orderNumber} Accepted!", Toast.LENGTH_SHORT).show()
            },
            onRequestScanToPack = {
                scannerTarget = SupplierScannerTarget.PACKING_CHECKLIST_SCAN
            }
        )
    }

    // Modal 3: Dispatch Outbound Freight Dialog
    orderToDispatch?.let { order ->
        DispatchOutboundDialog(
            order = order,
            initialTracking = scannedTrackingForDispatch,
            isDarkTheme = isDarkTheme,
            onDismiss = {
                orderToDispatch = null
                scannedTrackingForDispatch = ""
            },
            onRequestScanTracking = {
                scannerTarget = SupplierScannerTarget.DISPATCH_TRACKING_SCAN
            },
            onConfirmDispatch = { tracking, carrier, eta, weight, note ->
                onMarkDelivered(order.id, tracking, carrier, eta, weight, note)
                orderToDispatch = null
                scannedTrackingForDispatch = ""
                Toast.makeText(context, "Dispatched ${order.orderNumber} via $carrier!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal 4: Create Order Bottom Sheet
    if (showCreateDialog) {
        CreateOrderBottomSheet(
            products = products,
            scannedSku = scannedSkuForNewOrder,
            isDarkTheme = isDarkTheme,
            onDismiss = {
                showCreateDialog = false
                scannedSkuForNewOrder = null
            },
            onRequestScanProduct = {
                showCreateDialog = false
                scannerTarget = SupplierScannerTarget.CREATE_ORDER_SKU_SCAN
            },
            onSave = { title, qty, price, arrival ->
                onCreateOrder(title, qty, price, arrival)
                showCreateDialog = false
                scannedSkuForNewOrder = null
                Toast.makeText(context, "Order issued to Middleman HQ!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal 5: Edit Order Dialog
    orderToEdit?.let { order ->
        EditOrderDialog(
            order = order,
            isDarkTheme = isDarkTheme,
            onDismiss = { orderToEdit = null },
            onSave = { title, qty, price ->
                onUpdateOrder(order.id, title, qty, price)
                orderToEdit = null
                Toast.makeText(context, "Updated ${order.orderNumber}!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal 6: Delete Order Confirmation Dialog
    orderToDelete?.let { order ->
        AlertDialog(
            containerColor = cardBg,
            onDismissRequest = { orderToDelete = null },
            title = { Text("Delete ${order.orderNumber}?", fontWeight = FontWeight.Bold, color = textPrimary) },
            text = { Text("Are you sure you want to remove this purchase order?", color = textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteOrder(order.id)
                        orderToDelete = null
                        Toast.makeText(context, "Order deleted.", Toast.LENGTH_SHORT).show()
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

    // Modal 7: Add Catalog Item Dialog
    if (showAddCatalogItemDialog) {
        AddCatalogProductDialog(
            initialSku = scannedSkuForCatalog,
            isDarkTheme = isDarkTheme,
            onDismiss = {
                showAddCatalogItemDialog = false
                scannedSkuForCatalog = ""
            },
            onRequestScanSku = {
                showAddCatalogItemDialog = false
                scannerTarget = SupplierScannerTarget.CATALOG_SKU_SCAN
            },
            onSave = { newProd ->
                onAddProduct(newProd)
                showAddCatalogItemDialog = false
                scannedSkuForCatalog = ""
                Toast.makeText(context, "Added '${newProd.name}' to Wholesale Catalog!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal 8: Edit Catalog Item Dialog
    catalogItemToEdit?.let { prod ->
        EditCatalogProductDialog(
            product = prod,
            isDarkTheme = isDarkTheme,
            onDismiss = { catalogItemToEdit = null },
            onSave = { updatedProd ->
                onUpdateProduct(updatedProd)
                catalogItemToEdit = null
                Toast.makeText(context, "Updated '${updatedProd.name}'!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
