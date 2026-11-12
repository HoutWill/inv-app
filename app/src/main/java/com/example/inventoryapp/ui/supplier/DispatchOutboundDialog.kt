package com.example.inventoryapp.ui.supplier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.inventoryapp.data.local.entity.PurchaseOrderEntity
import com.example.inventoryapp.ui.theme.*

/**
 * "Send Goods Out" Dispatch Screen/Sheet, 1:1 matching the Behance "Supply Chain Mastery" reference mockup.
 * Displays:
 * 1. Top bar: <- Send Goods Out
 * 2. Delivery Info card:
 *    - Connected route: Pickup Address -> Dropoff Address
 *    - (+) Add More Stops
 *    - Delivery Time (Clock icon)
 *    - Transportation (Car/Truck icon)
 *    - Package Items (Box icon)
 * 3. Order Info card:
 *    - Link QR / Barcode with Scan/Upload action
 *    - Movement Name field
 * 4. Full-width gradient "Send Goods Out" action button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispatchOutboundDialog(
    order: PurchaseOrderEntity,
    initialTracking: String = "",
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit,
    onRequestScanTracking: () -> Unit = {},
    onConfirmDispatch: (tracking: String, carrier: String, eta: String, weight: String, note: String) -> Unit
) {
    val bgMain = if (isDarkTheme) NawanoDark400 else NawanoNeutral50
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight
    val dividerColor = if (isDarkTheme) NawanoDarkOutline.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

    val defaultTracking = remember(order.id, initialTracking) {
        if (initialTracking.isNotBlank()) initialTracking
        else if (order.trackingNumber.isNotBlank()) order.trackingNumber
        else "TRK-${(1000..9999).random()}"
    }
    var trackingNumber by remember { mutableStateOf(defaultTracking) }
    var carrier by remember { mutableStateOf(if (order.picSender.isNotBlank()) order.picSender else "Pickup Car") }
    var etaWindow by remember { mutableStateOf(if (order.estArrival.isNotBlank()) order.estArrival else "Today, 10:00 - 11:00 AM") }
    var weight by remember { mutableStateOf(if (order.totalWeight.isNotBlank()) order.totalWeight else "27KG") }
    var movementName by remember {
        mutableStateOf("03 - Items ${order.title.ifBlank { "crab in from Toronto" }} weight: ${weight}")
    }

    var pickupAddress by remember { mutableStateOf("1550 Bedok North Ave 4, #04 30, Singapore") }
    var dropoffAddress by remember { mutableStateOf("01-33H, The Centrepoint, 176 Orchard Rd") }

    LaunchedEffect(initialTracking) {
        if (initialTracking.isNotBlank()) {
            trackingNumber = initialTracking
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = bgMain,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = if (isDarkTheme) NawanoDarkOutline else Color(0xFFCBD5E1)
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 24.dp)
            .imePadding()
    ) {
        Scaffold(
            containerColor = bgMain,
            topBar = {
                Surface(
                    color = bgMain,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Send Goods Out",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }
                }
            },
            bottomBar = {
                Surface(
                    color = bgMain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = {
                            onConfirmDispatch(trackingNumber, carrier, etaWindow, weight, movementName)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(NawanoGradientStart, NawanoGradientEnd)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Send Goods Out",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card 1: Delivery Info
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    shadowElevation = if (isDarkTheme) 0.dp else 1.5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Delivery Info",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Route with Connected Line and 2 Stops
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Left Column: Dots and Connector Line
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(top = 4.dp, end = 12.dp)
                            ) {
                                // Pickup Dot (Circle outline)
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Transparent,
                                    border = BorderStroke(2.dp, NawanoPrimary200),
                                    modifier = Modifier.size(12.dp)
                                ) {}

                                // Vertical Connecting Route Line
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(44.dp)
                                        .background(NawanoPrimary200.copy(alpha = 0.4f))
                                )

                                // Dropoff Dot (Filled pin circle)
                                Surface(
                                    shape = CircleShape,
                                    color = NawanoPrimary200,
                                    modifier = Modifier.size(12.dp)
                                ) {}
                            }

                            // Right Column: Addresses
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Pickup Address
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Pickup Address",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = textSecondary
                                        )
                                        Text(
                                            text = pickupAddress,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textPrimary,
                                            maxLines = 1
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = textSecondary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Dropoff Address
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Dropoff Address",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = textSecondary
                                        )
                                        Text(
                                            text = dropoffAddress,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textPrimary,
                                            maxLines = 1
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = textSecondary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // (+) Add More Stops
                        Text(
                            text = "+ Add More Stops",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NawanoPrimary200,
                            modifier = Modifier
                                .clickable { }
                                .padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = dividerColor)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Row 1: Delivery Time
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = NawanoPrimary200.copy(alpha = 0.12f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.AccessTime,
                                        contentDescription = null,
                                        tint = NawanoPrimary200,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Delivery Time",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textSecondary
                                )
                                Text(
                                    text = etaWindow,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = textSecondary.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = dividerColor)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Row 2: Transportation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = NawanoPrimary200.copy(alpha = 0.12f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.LocalShipping,
                                        contentDescription = null,
                                        tint = NawanoPrimary200,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Transportation",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textSecondary
                                )
                                Text(
                                    text = carrier,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Text(
                                    text = "Max. 150KG (200x200x120cm)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textSecondary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = textSecondary.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = dividerColor)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Row 3: Package Items
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = NawanoPrimary200.copy(alpha = 0.12f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.Inventory2,
                                        contentDescription = null,
                                        tint = NawanoPrimary200,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Package Items",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textSecondary
                                )
                                Text(
                                    text = "${order.totalQuantity} Items (${order.title.ifBlank { "Wholesale Stock" }})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Text(
                                    text = "Total Weight: $weight",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textSecondary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = textSecondary.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Card 2: Order Info
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    shadowElevation = if (isDarkTheme) 0.dp else 1.5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column {
                            Text(
                                text = "Order Info",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Provide more details in the shipping process of this order",
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondary
                            )
                        }

                        // Link QR / Barcode Card
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDarkTheme) NawanoDark400 else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = NawanoPrimary200.copy(alpha = 0.15f),
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.QrCode,
                                                contentDescription = null,
                                                tint = NawanoPrimary200,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = "Link QR / Barcode",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary
                                        )
                                        Text(
                                            text = if (trackingNumber.isNotBlank()) trackingNumber else "No Code Linked",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = textSecondary
                                        )
                                    }
                                }

                                Surface(
                                    onClick = onRequestScanTracking,
                                    shape = RoundedCornerShape(10.dp),
                                    color = NawanoPrimary200
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "Scan",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Movement Name Outlined Field
                        OutlinedTextField(
                            value = movementName,
                            onValueChange = { movementName = it },
                            label = { Text("Movement Name", color = textSecondary) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                focusedBorderColor = NawanoPrimary200,
                                unfocusedBorderColor = cardBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
