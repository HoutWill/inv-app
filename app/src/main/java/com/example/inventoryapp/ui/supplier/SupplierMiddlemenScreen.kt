package com.example.inventoryapp.ui.supplier

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.PartnerConnectionWithUser
import com.example.inventoryapp.data.local.entity.PurchaseOrderEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderStatus
import com.example.inventoryapp.data.local.entity.SupplierIssueEntity
import com.example.inventoryapp.data.local.entity.SupplierIssueStatus
import com.example.inventoryapp.data.local.entity.UserEntity
import com.example.inventoryapp.data.local.entity.UserRole
import com.example.inventoryapp.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

data class MiddlemanDirectoryEntry(
    val id: String,
    val displayName: String,
    val uniqueCode: String,
    val email: String,
    val phone: String,
    val isConnectedPartner: Boolean,
    val ordersCount: Int,
    val activeOrdersCount: Int,
    val totalVolumeUsd: Double,
    val openIssuesCount: Int,
    val totalIssuesCount: Int
)

/**
 * Combines connected partner Middlemen, registered Middleman user accounts,
 * and Middlemen appearing on Purchase Orders / Issue Reports into one unified directory.
 */
fun buildMiddlemanDirectory(
    middlemanUsers: List<UserEntity>,
    activeConnections: List<PartnerConnectionWithUser>,
    orders: List<PurchaseOrderEntity>,
    issues: List<SupplierIssueEntity>
): List<MiddlemanDirectoryEntry> {
    val connectedPartnerIds = activeConnections
        .filter { it.partnerUser.role == UserRole.OWNER }
        .map { it.partnerUser.id }
        .toSet()

    val entriesById = linkedMapOf<String, MiddlemanDirectoryEntry>()

    // 1. Registered / Connected Middleman accounts
    val allUsers = (activeConnections.map { it.partnerUser } + middlemanUsers)
        .filter { it.role == UserRole.OWNER }
        .distinctBy { it.id }

    allUsers.forEach { u ->
        val cleanName = u.fullName.replace(" (Middle Man)", "").trim().ifBlank { u.fullName }
        val matchingOrders = orders.filter { order ->
            val pic = order.picSender.trim()
            pic.contains(cleanName, ignoreCase = true) ||
                cleanName.contains(pic, ignoreCase = true) ||
                (u.id == "user_middleman" && (pic.isBlank() || order.ownerNote.contains("Nawano HQ", ignoreCase = true))) ||
                (u.id == "user_middleman_david" && order.orderNumber.contains("0843"))
        }
        val matchingIssues = issues.filter { iss ->
            iss.middlemanId == u.id ||
                iss.middlemanCode.equals(u.uniqueCode, ignoreCase = true) ||
                iss.middlemanName.contains(cleanName, ignoreCase = true)
        }

        entriesById[u.id] = MiddlemanDirectoryEntry(
            id = u.id,
            displayName = cleanName,
            uniqueCode = u.uniqueCode.ifBlank { "MID-1001" },
            email = u.email,
            phone = u.phoneNumber,
            isConnectedPartner = u.id in connectedPartnerIds,
            ordersCount = matchingOrders.size,
            activeOrdersCount = matchingOrders.count {
                it.status == PurchaseOrderStatus.PENDING ||
                    it.status == PurchaseOrderStatus.CONFIRMED ||
                    it.status == PurchaseOrderStatus.DELIVERED
            },
            totalVolumeUsd = matchingOrders.sumOf { it.totalAmount },
            openIssuesCount = matchingIssues.count {
                it.status == SupplierIssueStatus.OPEN ||
                    it.status == SupplierIssueStatus.ACKNOWLEDGED ||
                    it.status == SupplierIssueStatus.REPLACEMENT_SENT
            },
            totalIssuesCount = matchingIssues.size
        )
    }

    // 2. Include any additional Middlemen appearing only in orders/issues
    orders.map { it.picSender.trim() }.filter { it.isNotBlank() }.distinct().forEach { pic ->
        val alreadyListed = entriesById.values.any {
            it.displayName.contains(pic, ignoreCase = true) || pic.contains(it.displayName, ignoreCase = true)
        }
        if (!alreadyListed) {
            val matchingOrders = orders.filter { it.picSender.equals(pic, ignoreCase = true) }
            val matchingIssues = issues.filter { it.middlemanName.contains(pic, ignoreCase = true) }
            val syntheticId = "mm_${pic.lowercase().replace(Regex("[^a-z0-9]"), "_")}"
            entriesById[syntheticId] = MiddlemanDirectoryEntry(
                id = syntheticId,
                displayName = pic,
                uniqueCode = "MID-BUYER",
                email = "procurement@nawano.com",
                phone = "",
                isConnectedPartner = false,
                ordersCount = matchingOrders.size,
                activeOrdersCount = matchingOrders.count {
                    it.status == PurchaseOrderStatus.PENDING ||
                        it.status == PurchaseOrderStatus.CONFIRMED ||
                        it.status == PurchaseOrderStatus.DELIVERED
                },
                totalVolumeUsd = matchingOrders.sumOf { it.totalAmount },
                openIssuesCount = matchingIssues.count { it.status == SupplierIssueStatus.OPEN },
                totalIssuesCount = matchingIssues.size
            )
        }
    }

    return entriesById.values.sortedWith(
        compareByDescending<MiddlemanDirectoryEntry> { it.openIssuesCount }
            .thenByDescending { it.isConnectedPartner }
            .thenByDescending { it.ordersCount }
    )
}

/**
 * Supplier Portal Screen: Middleman List / Buyers Directory.
 * Mirrors Middleman HQ's SuppliersManagementScreen, showing all connected and active Middlemen,
 * order volumes, open issues count, and quick actions.
 */
@Composable
fun SupplierMiddlemenScreen(
    middlemen: List<MiddlemanDirectoryEntry>,
    orders: List<PurchaseOrderEntity>,
    issues: List<SupplierIssueEntity>,
    pendingPartnerRequestsCount: Int = 0,
    isDarkTheme: Boolean = false,
    onOpenConnectPartner: () -> Unit = {},
    onViewOrdersForMiddleman: (String) -> Unit = {},
    onViewIssuesForMiddleman: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableIntStateOf(0) } // 0: All, 1: Connected, 2: With Issues
    var selectedMiddleman by remember { mutableStateOf<MiddlemanDirectoryEntry?>(null) }

    val filteredMiddlemen = remember(middlemen, searchQuery, selectedFilter) {
        middlemen.filter { mm ->
            when (selectedFilter) {
                1 -> mm.isConnectedPartner
                2 -> mm.openIssuesCount > 0 || mm.totalIssuesCount > 0
                else -> true
            }
        }.filter { mm ->
            searchQuery.isBlank() ||
                mm.displayName.contains(searchQuery, ignoreCase = true) ||
                mm.uniqueCode.contains(searchQuery, ignoreCase = true) ||
                mm.email.contains(searchQuery, ignoreCase = true) ||
                mm.phone.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 140.dp)
    ) {
        // Title & Summary Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Middleman Directory",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textPrimary
                )
                Text(
                    text = "${middlemen.size} buyers • ${middlemen.count { it.isConnectedPartner }} verified partner connections",
                    fontSize = 12.5.sp,
                    color = textSecondary
                )
            }
        }

        // Search Bar
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDarkTheme) Color(0xFF1E2235) else Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF2E334D) else Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Middlemen",
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        cursorBrush = SolidColor(NawanoPrimary200),
                        textStyle = TextStyle(
                            color = textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search middleman name, MID code, phone...",
                                    color = textSecondary.copy(alpha = 0.75f),
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Connect Middleman by ID / Phone Banner
        item {
            Surface(
                onClick = onOpenConnectPartner,
                shape = RoundedCornerShape(14.dp),
                color = cardBg,
                border = BorderStroke(1.dp, NawanoPrimary200.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = NawanoPrimary200.copy(alpha = 0.14f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = NawanoPrimary200,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "Connect Middleman by ID or Phone",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Search Unique ID (MID-XXXX) or Phone to link",
                                fontSize = 11.sp,
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (pendingPartnerRequestsCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NawanoRose,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "$pendingPartnerRequestsCount pending",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = NawanoPrimary200,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Filter Chips
        item {
            val chips = listOf(
                "All (${middlemen.size})",
                "Connected (${middlemen.count { it.isConnectedPartner }})",
                "With Issues (${middlemen.count { it.openIssuesCount > 0 || it.totalIssuesCount > 0 }})"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chips.forEachIndexed { index, label ->
                    val isSelected = selectedFilter == index
                    Surface(
                        onClick = { selectedFilter = index },
                        shape = CircleShape,
                        color = if (isSelected) NawanoPrimary200 else cardBg,
                        border = if (isSelected) null else BorderStroke(1.dp, cardBorder)
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else textSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        // List
        if (filteredMiddlemen.isEmpty()) {
            item {
                EmptyZeroState(
                    title = "No Middlemen Found",
                    subtitle = "Connect with a Middleman partner using their MID code or phone number.",
                    icon = Icons.Default.Groups,
                    isDarkTheme = isDarkTheme,
                    actionLabel = "+ Connect Middleman",
                    onAction = onOpenConnectPartner
                )
            }
        } else {
            items(filteredMiddlemen, key = { it.id }) { mm ->
                MiddlemanDirectoryCard(
                    entry = mm,
                    isDarkTheme = isDarkTheme,
                    onCardClick = { selectedMiddleman = mm },
                    onViewOrders = { onViewOrdersForMiddleman(mm.displayName) },
                    onViewIssues = { onViewIssuesForMiddleman(mm.displayName) }
                )
            }
        }
    }

    selectedMiddleman?.let { mm ->
        val relatedOrders = orders.filter {
            it.picSender.contains(mm.displayName, ignoreCase = true) ||
                mm.displayName.contains(it.picSender, ignoreCase = true)
        }
        val relatedIssues = issues.filter {
            it.middlemanId == mm.id ||
                it.middlemanCode.equals(mm.uniqueCode, ignoreCase = true) ||
                it.middlemanName.contains(mm.displayName, ignoreCase = true)
        }

        MiddlemanDetailBottomSheet(
            entry = mm,
            relatedOrders = relatedOrders,
            relatedIssues = relatedIssues,
            isDarkTheme = isDarkTheme,
            onDismiss = { selectedMiddleman = null },
            onViewOrders = {
                selectedMiddleman = null
                onViewOrdersForMiddleman(mm.displayName)
            },
            onViewIssues = {
                selectedMiddleman = null
                onViewIssuesForMiddleman(mm.displayName)
            }
        )
    }
}

@Composable
private fun MiddlemanDirectoryCard(
    entry: MiddlemanDirectoryEntry,
    isDarkTheme: Boolean,
    onCardClick: () -> Unit,
    onViewOrders: () -> Unit,
    onViewIssues: () -> Unit
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    val initials = remember(entry.displayName) {
        entry.displayName.split(" ")
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .take(2)
            .joinToString("")
            .ifBlank { "MM" }
    }

    Surface(
        onClick = onCardClick,
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isDarkTheme) 0.dp else 1.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row 1: Avatar + Name + Code + Connected Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(NawanoPrimary200.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = NawanoPrimary200,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.displayName,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = entry.uniqueCode,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = NawanoPrimary200
                            )
                            if (entry.phone.isNotBlank()) {
                                Text(
                                    text = "• ${entry.phone}",
                                    fontSize = 11.sp,
                                    color = textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (entry.isConnectedPartner) {
                        if (isDarkTheme) Color(0xFF064E3B).copy(alpha = 0.45f) else Color(0xFFD1FAE5)
                    } else {
                        if (isDarkTheme) NawanoDarkCard else Color(0xFFF1F5F9)
                    }
                ) {
                    Text(
                        text = if (entry.isConnectedPartner) "Connected" else "Buyer",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (entry.isConnectedPartner) {
                            if (isDarkTheme) Color(0xFF34D399) else Color(0xFF059669)
                        } else {
                            textSecondary
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = cardBorder.copy(alpha = 0.5f))

            // Row 2: Order Metrics & Issues Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("Orders", fontSize = 10.5.sp, color = textSecondary)
                        Text("${entry.ordersCount}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = textPrimary)
                    }
                    Column {
                        Text("Volume", fontSize = 10.5.sp, color = textSecondary)
                        Text(
                            text = "$${String.format(Locale.US, "%,.0f", entry.totalVolumeUsd)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF10B981)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = onViewIssues,
                        shape = RoundedCornerShape(12.dp),
                        color = if (entry.openIssuesCount > 0) Color(0xFFEF4444).copy(alpha = 0.14f) else cardBorder.copy(alpha = 0.35f),
                        border = BorderStroke(
                            1.dp,
                            if (entry.openIssuesCount > 0) Color(0xFFEF4444).copy(alpha = 0.4f) else cardBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ReportProblem,
                                contentDescription = null,
                                tint = if (entry.openIssuesCount > 0) Color(0xFFEF4444) else textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (entry.openIssuesCount > 0) "${entry.openIssuesCount} Issues" else "Issues (${entry.totalIssuesCount})",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (entry.openIssuesCount > 0) Color(0xFFEF4444) else textSecondary
                            )
                        }
                    }

                    Surface(
                        onClick = onViewOrders,
                        shape = RoundedCornerShape(12.dp),
                        color = NawanoPrimary200
                    ) {
                        Text(
                            text = "Orders",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Swipe-up Modal Bottom Sheet showing full Middleman profile, contact info,
 * order history metrics, and open issues.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MiddlemanDetailBottomSheet(
    entry: MiddlemanDirectoryEntry,
    relatedOrders: List<PurchaseOrderEntity>,
    relatedIssues: List<SupplierIssueEntity>,
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit,
    onViewOrders: () -> Unit,
    onViewIssues: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberSafeModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val innerBg = if (isDarkTheme) NawanoDarkCard else Color(0xFFF8FAFC)
    val borderCol = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    fun animateDismiss() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = cardBg,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = textSecondary.copy(alpha = 0.4f))
        },
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.displayName,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Partner Code: ${entry.uniqueCode}",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NawanoPrimary200,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Copy Code",
                            tint = NawanoPrimary200,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Middleman Code", entry.uniqueCode))
                                    Toast.makeText(context, "Copied ${entry.uniqueCode}", Toast.LENGTH_SHORT).show()
                                }
                        )
                    }
                }
                IconButton(onClick = { animateDismiss() }) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = textSecondary)
                }
            }

            // Contact Info Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = innerBg,
                border = BorderStroke(1.dp, borderCol),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("CONTACT DETAILS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                    if (entry.email.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = NawanoPrimary200, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(entry.email, fontSize = 13.sp, color = textPrimary)
                        }
                    }
                    if (entry.phone.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = NawanoPrimary200, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(entry.phone, fontSize = 13.sp, color = textPrimary)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("TOTAL ORDERS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                            Text("${relatedOrders.size} POs (${entry.activeOrdersCount} active)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("ISSUE REPORTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                            Text(
                                text = "${entry.openIssuesCount} Open / ${relatedIssues.size} Total",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (entry.openIssuesCount > 0) Color(0xFFEF4444) else Color(0xFF10B981)
                            )
                        }
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onViewIssues,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (entry.openIssuesCount > 0) Color(0xFFEF4444) else borderCol),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (entry.openIssuesCount > 0) Color(0xFFEF4444) else textPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Outlined.ReportProblem, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("View Issues (${relatedIssues.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = onViewOrders,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("View Orders (${entry.ordersCount})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
