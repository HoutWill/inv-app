package com.example.inventoryapp.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onBack: () -> Unit,
    onScanClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val filterTabs = listOf("All Activity", "Incoming Req", "Inbound Goods", "Outbound Goods")
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    // Seed mock and real activities
    val sampleActivities = remember(stockTransactions) {
        val list = mutableListOf(
            MovementActivityItem(
                id = "1",
                type = MovementType.INBOUND,
                referenceCode = "MOV-2024-0891",
                title = "Wholesale Inbound Delivery",
                partnerOrLocation = "Supplier: Tokyo Fresh Distribution",
                timestamp = System.currentTimeMillis() - 3600_000 * 2,
                itemsCount = 6,
                totalQty = 240,
                status = MovementStatus.COMPLETED
            ),
            MovementActivityItem(
                id = "2",
                type = MovementType.OUTBOUND,
                referenceCode = "ORD-2024-1104",
                title = "Store Dispatch Order",
                partnerOrLocation = "Destination: Downtown Outlet #2",
                timestamp = System.currentTimeMillis() - 3600_000 * 5,
                itemsCount = 4,
                totalQty = 85,
                status = MovementStatus.IN_DELIVERY
            ),
            MovementActivityItem(
                id = "3",
                type = MovementType.INCOMING_REQ,
                referenceCode = "REQ-2024-0412",
                title = "Restock Requisition",
                partnerOrLocation = "Branch: Central Kitchen Hub",
                timestamp = System.currentTimeMillis() - 3600_000 * 24,
                itemsCount = 8,
                totalQty = 310,
                status = MovementStatus.PENDING
            ),
            MovementActivityItem(
                id = "4",
                type = MovementType.INBOUND,
                referenceCode = "MOV-2024-0870",
                title = "Cold Chain Poultry Cargo",
                partnerOrLocation = "Supplier: Green Valley Meats",
                timestamp = System.currentTimeMillis() - 3600_000 * 48,
                itemsCount = 3,
                totalQty = 150,
                status = MovementStatus.COMPLETED
            ),
            MovementActivityItem(
                id = "5",
                type = MovementType.OUTBOUND,
                referenceCode = "ORD-2024-1088",
                title = "POS Counter Restock",
                partnerOrLocation = "Destination: Westside Franchise",
                timestamp = System.currentTimeMillis() - 3600_000 * 72,
                itemsCount = 5,
                totalQty = 120,
                status = MovementStatus.COMPLETED
            )
        )

        // Augment with recent stock transactions if available
        stockTransactions.take(5).forEachIndexed { idx, tx ->
            val isStockIn = tx.quantityDelta > 0
            val codeSuffix = if (tx.id.length >= 6) tx.id.take(6).uppercase() else tx.id.uppercase()
            list.add(
                MovementActivityItem(
                    id = "tx_${tx.id}",
                    type = if (isStockIn) MovementType.INBOUND else MovementType.OUTBOUND,
                    referenceCode = "TX-$codeSuffix",
                    title = "${if (isStockIn) "Stock In" else "Stock Out"}: ${tx.productName}",
                    partnerOrLocation = if (!tx.note.isNullOrBlank()) tx.note else "Quick adjustment",
                    timestamp = tx.timestamp,
                    itemsCount = 1,
                    totalQty = kotlin.math.abs(tx.quantityDelta),
                    status = MovementStatus.COMPLETED
                )
            )
        }
        list
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
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
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Horizontal Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterTabs.forEachIndexed { index, tabName ->
                    val isSelected = selectedFilterIndex == index
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilterIndex = index },
                        label = {
                            Text(
                                text = tabName,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NawanoPrimary200,
                            selectedLabelColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    )
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
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add Activity",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
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
            filteredActivities.forEach { item ->
                MovementCard(item = item)
            }
        }
    }

    if (showCreateDialog) {
        CreateMovementDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { showCreateDialog = false }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = item.status.bg
                ) {
                    Text(
                        text = item.status.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = item.status.fg,
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
                color = MaterialTheme.colorScheme.onSurface
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
                        color = NawanoPrimary50
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
                        color = NawanoPrimary50
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

@Composable
private fun CreateMovementDialog(
    onDismiss: () -> Unit,
    onCreate: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var movementType by remember { mutableStateOf(MovementType.INBOUND) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add New Movement Activity",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Log a logistics request, inbound shipment, or branch transfer.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Activity Title / Purpose") },
                    placeholder = { Text("e.g. Supplier Inbound Restock") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location / Partner") },
                    placeholder = { Text("e.g. Warehouse Bay 3") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onCreate,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200)
            ) {
                Text("Log Activity")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
