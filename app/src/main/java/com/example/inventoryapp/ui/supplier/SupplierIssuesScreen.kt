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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.IssueSeverity
import com.example.inventoryapp.data.local.entity.SupplierIssueEntity
import com.example.inventoryapp.data.local.entity.SupplierIssueStatus
import com.example.inventoryapp.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dedicated screen in the Supplier Portal displaying Issue Reports
 * received from Middlemen, with filtering, middleman drill-down,
 * and resolution workflow.
 */
@Composable
fun SupplierIssuesScreen(
    issues: List<SupplierIssueEntity>,
    initialMiddlemanFilter: String? = null,
    isDarkTheme: Boolean = false,
    onUpdateIssueStatus: (issueId: String, newStatus: SupplierIssueStatus, response: String) -> Unit,
    onFilterOrdersByMiddleman: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableIntStateOf(0) } // 0: All, 1: Open, 2: In Progress, 3: Resolved
    var selectedMiddlemanFilter by remember(initialMiddlemanFilter) { mutableStateOf(initialMiddlemanFilter) }
    var showMiddlemanMenu by remember { mutableStateOf(false) }
    var isSortOldestFirst by remember { mutableStateOf(false) }
    var selectedIssue by remember { mutableStateOf<SupplierIssueEntity?>(null) }

    val openCount = remember(issues) { issues.count { it.status == SupplierIssueStatus.OPEN } }
    val inProgressCount = remember(issues) {
        issues.count { it.status == SupplierIssueStatus.ACKNOWLEDGED || it.status == SupplierIssueStatus.REPLACEMENT_SENT }
    }
    val resolvedCount = remember(issues) {
        issues.count { it.status == SupplierIssueStatus.RESOLVED || it.status == SupplierIssueStatus.DISMISSED }
    }

    val availableMiddlemen = remember(issues) {
        issues.map { it.middlemanName.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    val filterTabs = remember(issues.size, openCount, inProgressCount, resolvedCount) {
        listOf(
            "All (${issues.size})",
            "Open ($openCount)",
            "In Progress ($inProgressCount)",
            "Resolved ($resolvedCount)"
        )
    }

    val filteredIssues = remember(issues, selectedStatusFilter, selectedMiddlemanFilter, searchQuery, isSortOldestFirst) {
        val list = issues.filter { issue ->
            when (selectedStatusFilter) {
                1 -> issue.status == SupplierIssueStatus.OPEN
                2 -> issue.status == SupplierIssueStatus.ACKNOWLEDGED || issue.status == SupplierIssueStatus.REPLACEMENT_SENT
                3 -> issue.status == SupplierIssueStatus.RESOLVED || issue.status == SupplierIssueStatus.DISMISSED
                else -> true
            }
        }.filter { issue ->
            if (selectedMiddlemanFilter.isNullOrBlank()) true
            else issue.middlemanName.contains(selectedMiddlemanFilter!!, ignoreCase = true) ||
                issue.middlemanCode.equals(selectedMiddlemanFilter, ignoreCase = true)
        }.filter { issue ->
            if (searchQuery.isBlank()) true
            else issue.issueCode.contains(searchQuery, ignoreCase = true) ||
                issue.orderNumber.contains(searchQuery, ignoreCase = true) ||
                issue.middlemanName.contains(searchQuery, ignoreCase = true) ||
                issue.issueType.contains(searchQuery, ignoreCase = true) ||
                issue.affectedItemName.contains(searchQuery, ignoreCase = true) ||
                issue.description.contains(searchQuery, ignoreCase = true)
        }

        if (isSortOldestFirst) list.sortedBy { it.createdAt }
        else list
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 140.dp)
    ) {
        // Hero Header Banner
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
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
                                    Color(0xFFDC2626),
                                    Color(0xFF9333EA),
                                    NawanoPrimary200
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.ReportProblem,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Middleman Issue Reports",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Discrepancies & damage claims from buyers",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Open Action", fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.78f))
                                Text("$openCount", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("In Progress", fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.78f))
                                Text("$inProgressCount", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Resolved", fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.78f))
                                Text("$resolvedCount", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }
                }
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
                        contentDescription = "Search Issues",
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
                                    text = "Search #ISS, PO#, middleman, item...",
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

        // Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterTabs.forEachIndexed { index, label ->
                    val isSelected = selectedStatusFilter == index
                    Surface(
                        onClick = { selectedStatusFilter = index },
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

        // Section Header + Middleman & Sort Dropdowns
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Issues (${filteredIssues.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box {
                        Surface(
                            onClick = { showMiddlemanMenu = true },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isDarkTheme) Color(0xFF1E2235) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, cardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = selectedMiddlemanFilter ?: "All Middlemen",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showMiddlemanMenu,
                            onDismissRequest = { showMiddlemanMenu = false },
                            modifier = Modifier.background(cardBg)
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Middlemen", color = textPrimary) },
                                onClick = {
                                    selectedMiddlemanFilter = null
                                    showMiddlemanMenu = false
                                }
                            )
                            availableMiddlemen.forEach { mm ->
                                DropdownMenuItem(
                                    text = { Text(mm, color = textPrimary) },
                                    onClick = {
                                        selectedMiddlemanFilter = mm
                                        showMiddlemanMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = { isSortOldestFirst = !isSortOldestFirst },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDarkTheme) Color(0xFF1E2235) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, cardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isSortOldestFirst) "Oldest" else "Recent",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = textPrimary
                            )
                        }
                    }
                }
            }
        }

        // Issue Cards
        if (filteredIssues.isEmpty()) {
            item {
                EmptyZeroState(
                    title = "No Issue Reports Found",
                    subtitle = if (selectedMiddlemanFilter != null) {
                        "No issues reported by $selectedMiddlemanFilter matching this filter."
                    } else {
                        "No reported delivery or quality issues from Middleman HQ."
                    },
                    icon = Icons.Default.CheckCircleOutline,
                    isDarkTheme = isDarkTheme
                )
            }
        } else {
            items(filteredIssues, key = { it.id }) { issue ->
                SupplierIssueCard(
                    issue = issue,
                    isDarkTheme = isDarkTheme,
                    onClick = { selectedIssue = issue }
                )
            }
        }
    }

    selectedIssue?.let { issue ->
        // Keep selectedIssue synced with live Flow list
        val liveIssue = issues.find { it.id == issue.id } ?: issue
        SupplierIssueDetailSheet(
            issue = liveIssue,
            isDarkTheme = isDarkTheme,
            onDismiss = { selectedIssue = null },
            onUpdateStatus = { newStatus, response ->
                onUpdateIssueStatus(liveIssue.id, newStatus, response)
                Toast.makeText(
                    context,
                    "Issue ${liveIssue.issueCode} marked ${newStatus.name.replace('_', ' ')}",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onFilterOrdersByMiddleman = { mmName ->
                selectedIssue = null
                onFilterOrdersByMiddleman(mmName)
            }
        )
    }
}

@Composable
private fun SupplierIssueCard(
    issue: SupplierIssueEntity,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val cardBg = if (isDarkTheme) Color(0xFF161A2B) else Color.White
    val cardBorder = if (isDarkTheme) Color(0xFF262C44) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    val (statusLabel, statusBg, statusFg) = issueStatusStyle(issue.status, isDarkTheme)
    val (sevLabel, sevColor) = issueSeverityStyle(issue.severity)

    val dateStr = remember(issue.createdAt) {
        SimpleDateFormat("MMM dd, HH:mm", Locale.US).format(Date(issue.createdAt))
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isDarkTheme) 0.dp else 1.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Status + Severity on left, Code & Date on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = RoundedCornerShape(8.dp), color = statusBg) {
                        Text(
                            text = statusLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusFg,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = sevColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = sevLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = sevColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = "${issue.issueCode} • $dateStr",
                    fontSize = 11.sp,
                    color = textSecondary
                )
            }

            // Row 2: Issue Type & Item Breakdown
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (issue.affectedItemName.isNotBlank()) {
                        "${issue.issueType} • ${issue.affectedItemName}"
                    } else {
                        issue.issueType
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = issue.description,
                    fontSize = 13.sp,
                    color = textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Row 3: Middleman Reporter + Linked PO + Action Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = NawanoPrimary200,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "${issue.middlemanName} (${issue.middlemanCode})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (issue.orderNumber.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NawanoPrimary200.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = issue.orderNumber,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NawanoPrimary200,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (issue.status == SupplierIssueStatus.OPEN) Color(0xFFEF4444) else NawanoPrimary200.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = if (issue.status == SupplierIssueStatus.OPEN) "Review" else "Details",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (issue.status == SupplierIssueStatus.OPEN) Color.White else NawanoPrimary200,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

/**
 * Swipe-up Modal Bottom Sheet for inspecting a Middleman Issue Report
 * and posting Supplier acknowledgement, replacement dispatch, or resolution.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierIssueDetailSheet(
    issue: SupplierIssueEntity,
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit,
    onUpdateStatus: (SupplierIssueStatus, String) -> Unit,
    onFilterOrdersByMiddleman: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val sheetState = rememberSafeModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val innerBg = if (isDarkTheme) NawanoDarkCard else Color(0xFFF8FAFC)
    val borderCol = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    var responseText by remember(issue.id) { mutableStateOf(issue.supplierResponse) }
    val (statusLabel, statusBg, statusFg) = issueStatusStyle(issue.status, isDarkTheme)
    val (sevLabel, sevColor) = issueSeverityStyle(issue.severity)

    val quickReplies = remember {
        listOf(
            "Replacement batch queued for next delivery",
            "Credit note applied to Middleman invoice",
            "Investigating with cold-chain courier",
            "Verified & resolved with HQ"
        )
    }

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
            // Header
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
                        text = issue.issueCode,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Copy Issue Code",
                        tint = NawanoPrimary200,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Issue Code", issue.issueCode))
                                Toast.makeText(context, "Copied ${issue.issueCode}", Toast.LENGTH_SHORT).show()
                            }
                    )
                    Surface(shape = RoundedCornerShape(8.dp), color = statusBg) {
                        Text(
                            text = statusLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusFg,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = sevColor.copy(alpha = 0.14f)) {
                        Text(
                            text = sevLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = sevColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { animateDismiss() },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = textSecondary)
                }
            }

            // Middleman Reporter & Order Meta Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = innerBg,
                border = BorderStroke(1.dp, borderCol),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("REPORTED BY MIDDLEMAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                            Spacer(Modifier.height(2.dp))
                            Text(issue.middlemanName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            Text("ID: ${issue.middlemanCode}", fontSize = 11.5.sp, fontFamily = FontFamily.Monospace, color = NawanoPrimary200)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("LINKED ORDER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = issue.orderNumber.ifBlank { "General Account" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.US).format(Date(issue.createdAt)),
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }
                    }

                    HorizontalDivider(color = borderCol.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ISSUE TYPE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                            Text(issue.issueType, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                        }
                        if (issue.affectedItemName.isNotBlank()) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("AFFECTED ITEM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                                Text(
                                    text = "${issue.affectedItemName} (${issue.affectedQuantity} units)",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = borderCol.copy(alpha = 0.5f))

                    Column {
                        Text("MIDDLEMAN REPORT DESCRIPTION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                        Spacer(Modifier.height(4.dp))
                        Text(issue.description, fontSize = 13.5.sp, color = textPrimary)
                    }
                }
            }

            // Supplier Response Input + Quick Templates
            if (issue.status != SupplierIssueStatus.RESOLVED && issue.status != SupplierIssueStatus.DISMISSED) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "SUPPLIER RESOLUTION RESPONSE",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary,
                        letterSpacing = 0.8.sp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickReplies.forEach { reply ->
                            Surface(
                                onClick = { responseText = reply },
                                shape = RoundedCornerShape(10.dp),
                                color = NawanoPrimary200.copy(alpha = 0.10f),
                                border = BorderStroke(1.dp, NawanoPrimary200.copy(alpha = 0.25f))
                            ) {
                                Text(
                                    text = "+ $reply",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NawanoPrimary200,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = responseText,
                        onValueChange = { responseText = it },
                        label = { Text("Response / Action Taken for Middleman") },
                        placeholder = { Text("Enter replacement tracking or resolution note...") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Action Buttons
                if (issue.status == SupplierIssueStatus.OPEN) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onUpdateStatus(
                                    SupplierIssueStatus.ACKNOWLEDGED,
                                    responseText.ifBlank { "Issue acknowledged by supplier desk." }
                                )
                                animateDismiss()
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706)),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("Acknowledge", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                onUpdateStatus(
                                    SupplierIssueStatus.REPLACEMENT_SENT,
                                    responseText.ifBlank { "Replacement batch dispatched to Middleman." }
                                )
                                animateDismiss()
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("Send Replacement", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Button(
                    onClick = {
                        onUpdateStatus(
                            SupplierIssueStatus.RESOLVED,
                            responseText.ifBlank { "Issue resolved with Middleman HQ." }
                        )
                        animateDismiss()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Mark Issue Resolved", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                }
            } else {
                // Already resolved response display
                if (issue.supplierResponse.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "SUPPLIER RESOLUTION NOTE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(issue.supplierResponse, fontSize = 13.sp, color = textPrimary)
                        }
                    }
                }
            }

            TextButton(
                onClick = { onFilterOrdersByMiddleman(issue.middlemanName) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = NawanoPrimary200, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "View All Orders from ${issue.middlemanName}",
                    color = NawanoPrimary200,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun issueStatusStyle(status: SupplierIssueStatus, isDarkTheme: Boolean): Triple<String, Color, Color> {
    return when (status) {
        SupplierIssueStatus.OPEN -> Triple(
            "Open",
            if (isDarkTheme) Color(0xFF7F1D1D).copy(alpha = 0.45f) else Color(0xFFFEE2E2),
            if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFDC2626)
        )
        SupplierIssueStatus.ACKNOWLEDGED -> Triple(
            "Acknowledged",
            if (isDarkTheme) Color(0xFF78350F).copy(alpha = 0.45f) else Color(0xFFFEF3C7),
            if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFD97706)
        )
        SupplierIssueStatus.REPLACEMENT_SENT -> Triple(
            "Replacement Sent",
            if (isDarkTheme) Color(0xFF312E81).copy(alpha = 0.5f) else Color(0xFFE0E7FF),
            if (isDarkTheme) Color(0xFFC7D2FE) else Color(0xFF4338CA)
        )
        SupplierIssueStatus.RESOLVED -> Triple(
            "Resolved",
            if (isDarkTheme) Color(0xFF064E3B).copy(alpha = 0.45f) else Color(0xFFD1FAE5),
            if (isDarkTheme) Color(0xFF34D399) else Color(0xFF059669)
        )
        SupplierIssueStatus.DISMISSED -> Triple(
            "Dismissed",
            if (isDarkTheme) Color(0xFF374151).copy(alpha = 0.45f) else Color(0xFFF3F4F6),
            if (isDarkTheme) Color(0xFF9CA3AF) else Color(0xFF4B5563)
        )
    }
}

private fun issueSeverityStyle(severity: IssueSeverity): Pair<String, Color> {
    return when (severity) {
        IssueSeverity.CRITICAL -> "CRITICAL" to Color(0xFFDC2626)
        IssueSeverity.HIGH -> "HIGH" to Color(0xFFF97316)
        IssueSeverity.MEDIUM -> "MEDIUM" to Color(0xFFD97706)
        IssueSeverity.LOW -> "LOW" to Color(0xFF3B82F6)
    }
}
