package com.example.inventoryapp.ui.supplier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.SupplierEntity
import com.example.inventoryapp.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliersManagementScreen(
    suppliers: List<SupplierEntity>,
    onBack: () -> Unit,
    onSaveSupplier: (SupplierEntity) -> Unit,
    onToggleSupplierActive: (String, Boolean) -> Unit,
    onNewPurchaseOrder: (SupplierEntity) -> Unit,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    modifier: Modifier = Modifier
) {
    val bgMain = if (isDarkTheme) NawanoDark400 else NawanoNeutral50
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    var searchQuery by remember { mutableStateOf("") }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingSupplier by remember { mutableStateOf<SupplierEntity?>(null) }

    val filteredSuppliers = remember(suppliers, searchQuery) {
        suppliers.filter { s ->
            searchQuery.isBlank() ||
                    s.name.contains(searchQuery, ignoreCase = true) ||
                    s.contactName.contains(searchQuery, ignoreCase = true) ||
                    s.email.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = bgMain,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Suppliers Directory",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgMain
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingSupplier = null
                    showAddEditDialog = true
                },
                containerColor = NawanoPrimary200,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Supplier")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search supplier by name or contact...", color = textSecondary) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = textSecondary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = textSecondary)
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary,
                    focusedBorderColor = NawanoPrimary200,
                    unfocusedBorderColor = cardBorder,
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (filteredSuppliers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.LocalShipping,
                            contentDescription = null,
                            tint = NawanoPrimary200.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "No suppliers found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Tap the + button below to add your first supplier.",
                            fontSize = 13.sp,
                            color = textSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredSuppliers, key = { it.id }) { supplier ->
                        SupplierCard(
                            supplier = supplier,
                            isDarkTheme = isDarkTheme,
                            onEdit = {
                                editingSupplier = supplier
                                showAddEditDialog = true
                            },
                            onToggleActive = { onToggleSupplierActive(supplier.id, !supplier.isActive) },
                            onNewOrder = { onNewPurchaseOrder(supplier) }
                        )
                    }
                }
            }
        }
    }

    if (showAddEditDialog) {
        SupplierAddEditDialog(
            supplier = editingSupplier,
            isDarkTheme = isDarkTheme,
            onDismiss = { showAddEditDialog = false },
            onConfirm = { saved ->
                onSaveSupplier(saved)
                showAddEditDialog = false
            }
        )
    }
}

@Composable
private fun SupplierCard(
    supplier: SupplierEntity,
    isDarkTheme: Boolean = false,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onNewOrder: () -> Unit
) {
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
    val dividerColor = if (isDarkTheme) NawanoDarkOutline.copy(alpha = 0.6f) else Color(0xFFF1F5F9)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        shadowElevation = if (isDarkTheme) 0.dp else 1.5.dp,
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (supplier.isActive) Color(0xFFEF4444) else Color(0xFF94A3B8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = supplier.name.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = supplier.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        if (supplier.contactName.isNotBlank()) {
                            Text(
                                text = "Contact: ${supplier.contactName}",
                                fontSize = 13.sp,
                                color = textSecondary
                            )
                        }
                    }
                }

                val activeBg = if (supplier.isActive) {
                    if (isDarkTheme) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFD1FAE5)
                } else {
                    if (isDarkTheme) NawanoDarkCard else Color(0xFFF1F5F9)
                }
                val activeFg = if (supplier.isActive) {
                    if (isDarkTheme) Color(0xFF34D399) else Color(0xFF047857)
                } else {
                    if (isDarkTheme) NawanoTextSecondaryDark else Color(0xFF64748B)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = activeBg,
                    modifier = Modifier.clickable { onToggleActive() }
                ) {
                    Text(
                        text = if (supplier.isActive) "Active" else "Inactive",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (supplier.phone.isNotBlank() || supplier.email.isNotBlank() || supplier.address.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = dividerColor)
                Spacer(Modifier.height(8.dp))

                if (supplier.phone.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = textSecondary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(text = supplier.phone, fontSize = 12.sp, color = textSecondary)
                    }
                }
                if (supplier.email.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = textSecondary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(text = supplier.email, fontSize = 12.sp, color = textSecondary)
                    }
                }
                if (supplier.address.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = textSecondary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(text = supplier.address, fontSize = 12.sp, color = textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Outlined.Edit, contentDescription = null, tint = textSecondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(text = "Edit", fontSize = 13.sp, color = textSecondary)
                }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = onNewOrder,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(text = "Create PO", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierAddEditDialog(
    supplier: SupplierEntity?,
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (SupplierEntity) -> Unit
) {
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight
    val surfaceColor = if (isDarkTheme) NawanoDark200 else Color.White
    val cardColor = if (isDarkTheme) NawanoDarkCard else Color(0xFFF8FAFC)
    val outlineColor = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)

    var name by remember { mutableStateOf(supplier?.name ?: "") }
    var contactName by remember { mutableStateOf(supplier?.contactName ?: "") }
    var phone by remember { mutableStateOf(supplier?.phone ?: "") }
    var email by remember { mutableStateOf(supplier?.email ?: "") }
    var address by remember { mutableStateOf(supplier?.address ?: "") }
    var note by remember { mutableStateOf(supplier?.note ?: "") }

    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = textPrimary,
        unfocusedTextColor = textPrimary,
        focusedBorderColor = NawanoPrimary200,
        unfocusedBorderColor = outlineColor,
        focusedContainerColor = cardColor,
        unfocusedContainerColor = cardColor,
        focusedLabelColor = NawanoPrimary200,
        unfocusedLabelColor = textSecondary
    )

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = surfaceColor,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = if (isDarkTheme) NawanoDarkOutline else Color(0xFFCBD5E1)
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 40.dp)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (supplier == null) "Add New Supplier" else "Edit Supplier",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = textPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = textSecondary)
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Company Name *") },
                singleLine = true,
                colors = tfColors,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = contactName,
                onValueChange = { contactName = it },
                label = { Text("Contact Person") },
                singleLine = true,
                colors = tfColors,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                singleLine = true,
                colors = tfColors,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                singleLine = true,
                colors = tfColors,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Physical / Warehouse Address") },
                colors = tfColors,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Category / Special Notes") },
                colors = tfColors,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Cancel", color = textSecondary)
                }

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val toSave = supplier?.copy(
                                name = name.trim(),
                                contactName = contactName.trim(),
                                phone = phone.trim(),
                                email = email.trim(),
                                address = address.trim(),
                                note = note.trim()
                            ) ?: SupplierEntity(
                                name = name.trim(),
                                contactName = contactName.trim(),
                                phone = phone.trim(),
                                email = email.trim(),
                                address = address.trim(),
                                note = note.trim()
                            )
                            onConfirm(toSave)
                        }
                    },
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                ) {
                    Text(
                        text = if (supplier == null) "Create Supplier" else "Save Changes",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
