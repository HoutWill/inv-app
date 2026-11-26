package com.example.inventoryapp.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.data.local.entity.StockTransactionEntity
import com.example.inventoryapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MovementActivityItem(
    val id: String,
    val type: MovementType,
    val referenceCode: String,
    val title: String,
    val partnerOrLocation: String,
    val timestamp: Long,
    val itemsCount: Int,
    val totalQty: Int,
    val status: MovementStatus
)

enum class MovementType(val label: String, val color: Color, val icon: ImageVector) {
    INCOMING_REQ("Incoming Req", Color(0xFFF59E0B), Icons.Default.Description),
    INBOUND("Inbound Goods", NawanoPrimary200, Icons.Default.MoveToInbox),
    OUTBOUND("Outbound Goods", Color(0xFF06B6D4), Icons.Default.Outbox)
}

enum class MovementStatus(val label: String, val bg: Color, val fg: Color) {
    COMPLETED("Completed", Color(0xFFD1FAE5), Color(0xFF047857)),
    IN_DELIVERY("In Delivery", Color(0xFFE0E7FF), Color(0xFF4338CA)),
    PENDING("Pending", Color(0xFFFEF3C7), Color(0xFFB45309))
}

/**
 * Authentic Nawano Movement List Screen (Behance: 174422339 Slide 5 & 6).
 * Displays logistics activities: Incoming Goods Requisitions, Inbound Shipments,
 * and Outbound Logistics Dispatches with status tracking and quick creation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementListScreen(
    stockTransactions: List<StockTransactionEntity> = emptyList(),
    products: List<ProductEntity> = emptyList(),
    onLogMovement: (product: ProductEntity, delta: Int, type: String, note: String) -> Unit = { _, _, _, _ -> },
    onBack: () -> Unit,
    onScanClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val filterTabs = listOf("All Activity", "Incoming", "Inbound", "Outbound")
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    // Map real activities from stock transactions
    val sampleActivities = remember(stockTransactions) {
        stockTransactions.map { tx ->
            val isStockIn = tx.quantityDelta > 0
            val codeSuffix = if (tx.id.length >= 6) tx.id.take(6).uppercase() else tx.id.uppercase()
            MovementActivityItem(
                id = "tx_${tx.id}",
                type = if (isStockIn) MovementType.INBOUND else MovementType.OUTBOUND,
                referenceCode = "TX-$codeSuffix",
                title = "${if (isStockIn) "Stock In" else "Stock Out"}: ${tx.productName}",
                partnerOrLocation = if (!tx.note.isNullOrBlank()) tx.note else "Direct stock adjustment",
                timestamp = tx.timestamp,
                itemsCount = 1,
                totalQty = kotlin.math.abs(tx.quantityDelta),
                status = MovementStatus.COMPLETED
            )
        }
    }

    val filteredActivities = remember(sampleActivities, selectedFilterIndex, searchQuery) {
        sampleActivities.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                    item.referenceCode.contains(searchQuery, ignoreCase = true) ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.partnerOrLocation.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilterIndex) {
                0 -> true
                1 -> item.type == MovementType.INCOMING_REQ
                2 -> item.type == MovementType.INBOUND
                3 -> item.type == MovementType.OUTBOUND
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Movement List",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onScanClicked,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan Document",
                            tint = NawanoPrimary200
                        )
                    }
                }
            }
        }

        // Body Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .padding(bottom = 60.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Horizontal Filter Pills (User Design: Capsule shape, solid purple when active, white with slate text when inactive)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                filterTabs.forEachIndexed { index, tabName ->
                    val isSelected = selectedFilterIndex == index
                    Surface(
                        onClick = { selectedFilterIndex = index },
                        shape = CircleShape,
                        color = if (isSelected) Color(0xFF6C63FF) else MaterialTheme.colorScheme.surface,
                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        shadowElevation = if (isSelected) 3.dp else 1.dp
                    ) {
                        Text(
                            text = tabName,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 13.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                            ),
                            color = if (isSelected) Color.White else Color(0xFF5E6582),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            // Search Bar & + Add New Activity Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search code, title...", style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NawanoPrimary200,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                )

                // Behance Slide 5 "+ Add New Activity" Button
                Button(
                    onClick = { showCreateDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NawanoPrimary200,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add Activity",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Text(
                text = "Recent Logistics Activities (${filteredActivities.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Activity Cards List
            if (filteredActivities.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SwapHoriz,
                            contentDescription = null,
                            tint = NawanoPrimary200,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Movement Records",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Logistics activities and stock adjustments will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                filteredActivities.forEach { item ->
                    MovementCard(item = item)
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateMovementDialog(
            products = products,
            onDismiss = { showCreateDialog = false },
            onCreate = { product, delta, type, note ->
                onLogMovement(product, delta, type, note)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun MovementCard(item: MovementActivityItem) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Type pill + Reference Code + Status pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = item.type.color.copy(alpha = 0.12f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = item.type.icon,
                                contentDescription = null,
                                tint = item.type.color,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = item.type.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = item.type.color
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "#${item.referenceCode}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = item.status.bg
                ) {
                    Text(
                        text = item.status.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = item.status.fg,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title & Location
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                lineHeight = 18.sp,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = item.partnerOrLocation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(10.dp))

            // Footer Row: Items Count, Units Qty, and Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${item.itemsCount} SKUs",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = NawanoPrimary200,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${item.totalQty} Units",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = NawanoPrimary200,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = dateFormat.format(Date(item.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateMovementDialog(
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onCreate: (product: ProductEntity, delta: Int, type: String, note: String) -> Unit
) {
    val sheetState = rememberSafeModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedProduct by remember(products) { mutableStateOf(products.firstOrNull()) }
    var productMenuExpanded by remember { mutableStateOf(false) }
    var isInbound by remember { mutableStateOf(true) }
    var qtyText by remember { mutableStateOf("1") }
    var locationOrReason by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                width = 44.dp,
                height = 5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Log Stock Movement",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Record an inbound restock or outbound dispatch against inventory.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Movement Type Toggle (Inbound vs Outbound)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = { isInbound = true; validationError = null },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isInbound) NawanoPrimary200.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, if (isInbound) NawanoPrimary200 else Color.Transparent),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Inbound (+ Stock)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isInbound) NawanoPrimary200 else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
                Surface(
                    onClick = { isInbound = false; validationError = null },
                    shape = RoundedCornerShape(12.dp),
                    color = if (!isInbound) Color(0xFF06B6D4).copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, if (!isInbound) Color(0xFF06B6D4) else Color.Transparent),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Outbound (- Stock)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!isInbound) Color(0xFF0891B2) else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }

            // Product Dropdown
            ExposedDropdownMenuBox(
                expanded = productMenuExpanded,
                onExpandedChange = { if (products.isNotEmpty()) productMenuExpanded = !productMenuExpanded }
            ) {
                OutlinedTextField(
                    value = selectedProduct?.let { "${it.name} (${it.quantity} ${it.unit})" } ?: "No products in inventory",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Product *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productMenuExpanded) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = productMenuExpanded,
                    onDismissRequest = { productMenuExpanded = false }
                ) {
                    products.forEach { prod ->
                        DropdownMenuItem(
                            text = { Text("${prod.name} (${prod.quantity} ${prod.unit})", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            onClick = {
                                selectedProduct = prod
                                productMenuExpanded = false
                                validationError = null
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = qtyText,
                onValueChange = { qtyText = it.filter { ch -> ch.isDigit() }; validationError = null },
                label = { Text("Quantity *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = locationOrReason,
                onValueChange = { locationOrReason = it },
                label = { Text("Location / Reason") },
                placeholder = { Text(if (isInbound) "e.g. Warehouse Bay 3 restock" else "e.g. Branch transfer to Outlet B") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            if (validationError != null) {
                Text(
                    text = validationError!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val prod = selectedProduct
                        if (prod == null) {
                            validationError = "Please add a product first."
                            return@Button
                        }
                        val qty = qtyText.toIntOrNull()
                        if (qty == null || qty <= 0) {
                            validationError = "Enter a valid quantity greater than 0."
                            return@Button
                        }
                        if (!isInbound && qty > prod.quantity) {
                            validationError = "Cannot dispatch $qty ${prod.unit}; only ${prod.quantity} available."
                            return@Button
                        }
                        val delta = if (isInbound) qty else -qty
                        val type = if (isInbound) "RESTOCK" else "DISPATCH"
                        val note = locationOrReason.trim().ifBlank {
                            if (isInbound) "Manual inbound restock" else "Manual outbound dispatch"
                        }
                        onCreate(prod, delta, type, note)
                    },
                    modifier = Modifier.weight(1.3f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200)
                ) {
                    Text("Log Activity", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
