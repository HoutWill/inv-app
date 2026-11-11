package com.example.inventoryapp.ui.supplier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
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
import com.example.inventoryapp.data.local.entity.UserEntity
import com.example.inventoryapp.ui.theme.*

/**
 * Supplier Settings & Profile Screen, matching the Middleman configuration view.
 */
@Composable
fun SupplierSettingsScreen(
    user: UserEntity,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    var carrierSyncEnabled by remember { mutableStateOf(true) }
    var autoConfirmLowStock by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Supplier Profile Information Card
        item {
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
                                .size(52.dp)
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
        }

        // Warehouse & Logistics Location
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Warehouse & Loading Dock",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = NawanoPrimary200, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "450 Harbor Way, Bay 4, Toronto, ON",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = textSecondary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "+1 (555) 234-8901 • Dispatch Desk",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary
                        )
                    }
                }
            }
        }

        // App & Portal Preferences
        item {
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { carrierSyncEnabled = !carrierSyncEnabled },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Carrier Tracking Sync", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = textPrimary)
                            Text("Auto-broadcast waybill telemetry to Middleman", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                        }
                        Switch(
                            checked = carrierSyncEnabled,
                            onCheckedChange = { carrierSyncEnabled = it }
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = cardBorder)
                    Spacer(Modifier.height(10.dp))

                    // Auto Confirm Reorder Requests
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { autoConfirmLowStock = !autoConfirmLowStock },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Auto-Accept Restock Requests", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = textPrimary)
                            Text("Instant confirmation for catalog items in stock", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                        }
                        Switch(
                            checked = autoConfirmLowStock,
                            onCheckedChange = { autoConfirmLowStock = it }
                        )
                    }
                }
            }
        }

        // Log Out Button
        item {
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
}
