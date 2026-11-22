package com.example.inventoryapp.ui.partner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
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
import com.example.inventoryapp.data.local.entity.PartnerConnectionWithUser
import com.example.inventoryapp.data.local.entity.UserEntity
import com.example.inventoryapp.data.local.entity.UserRole
import com.example.inventoryapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerRequestsBottomSheet(
    currentUser: UserEntity,
    incomingRequests: List<PartnerConnectionWithUser>,
    sentRequests: List<PartnerConnectionWithUser>,
    activeConnections: List<PartnerConnectionWithUser>,
    onAccept: (connectionId: String) -> Unit,
    onDecline: (connectionId: String) -> Unit,
    onCancelSent: (connectionId: String) -> Unit,
    onDisconnect: (connectionId: String) -> Unit,
    onOpenAddPartner: () -> Unit,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean = false
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableStateOf(0) } // 0: Incoming, 1: Sent, 2: Connected

    val bgCard = if (isDarkTheme) NawanoDark200 else Color.White
    val bgInner = if (isDarkTheme) NawanoDark400 else NawanoNeutral50
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight
    val borderCol = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = bgCard,
        dragHandle = { BottomSheetDefaults.DragHandle(color = textSecondary.copy(alpha = 0.4f)) },
        modifier = Modifier.navigationBarsPadding().imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "Partner Network",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Manage connections & requests",
                        fontSize = 12.sp,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalButton(
                    onClick = {
                        onDismiss()
                        onOpenAddPartner()
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = NawanoPrimary200,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(text = "Connect", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }

            Spacer(Modifier.height(14.dp))

            // Tab Selector
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = bgInner,
                border = BorderStroke(1.dp, borderCol),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(3.dp)) {
                    val tabs = listOf(
                        "Incoming (${incomingRequests.size})",
                        "Sent (${sentRequests.size})",
                        "Connected (${activeConnections.size})"
                    )
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Surface(
                            onClick = { selectedTab = index },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) NawanoPrimary200 else Color.Transparent,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // Incoming Requests
                    if (incomingRequests.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Outlined.MailOutline,
                            title = "No Incoming Requests",
                            subtitle = "When a partner adds you by ID or Phone, their invitation will appear here.",
                            buttonText = "Share My ID",
                            onButtonClick = {
                                onDismiss()
                                onOpenAddPartner()
                            },
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(incomingRequests, key = { it.connection.id }) { item ->
                                IncomingRequestCard(
                                    item = item,
                                    onAccept = { onAccept(item.connection.id) },
                                    onDecline = { onDecline(item.connection.id) },
                                    bgInner = bgInner,
                                    bgCard = bgCard,
                                    borderCol = borderCol,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Sent Requests
                    if (sentRequests.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.AutoMirrored.Outlined.Send,
                            title = "No Pending Sent Requests",
                            subtitle = "You haven't sent any pending invites.",
                            buttonText = "Add New Partner",
                            onButtonClick = {
                                onDismiss()
                                onOpenAddPartner()
                            },
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(sentRequests, key = { it.connection.id }) { item ->
                                SentRequestCard(
                                    item = item,
                                    onCancel = { onCancelSent(item.connection.id) },
                                    bgInner = bgInner,
                                    borderCol = borderCol,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // Active Connections
                    if (activeConnections.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Outlined.People,
                            title = "No Connected Partners Yet",
                            subtitle = "Connect with partners to unlock direct ordering and catalog sharing.",
                            buttonText = "Find Partners",
                            onButtonClick = {
                                onDismiss()
                                onOpenAddPartner()
                            },
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(activeConnections, key = { it.connection.id }) { item ->
                                ConnectedPartnerCard(
                                    item = item,
                                    onDisconnect = { onDisconnect(item.connection.id) },
                                    bgInner = bgInner,
                                    borderCol = borderCol,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun IncomingRequestCard(
    item: PartnerConnectionWithUser,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    bgInner: Color,
    bgCard: Color,
    borderCol: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgInner,
        border = BorderStroke(1.dp, borderCol),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = NawanoPrimary200,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = item.partnerUser.fullName.take(2).uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.partnerUser.fullName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.partnerUser.uniqueCode,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NawanoPrimary200,
                            maxLines = 1
                        )
                        if (item.partnerUser.phoneNumber.isNotBlank()) {
                            Text(text = " • ", color = textSecondary)
                            Text(
                                text = item.partnerUser.phoneNumber,
                                fontSize = 11.sp,
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (item.connection.requestNote.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = bgCard,
                    border = BorderStroke(1.dp, borderCol.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"${item.connection.requestNote}\"",
                        fontSize = 12.sp,
                        color = textSecondary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, borderCol),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = textSecondary)
                ) {
                    Text(text = "Decline", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NawanoPrimary200,
                        contentColor = Color.White
                    )
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(text = "Accept", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SentRequestCard(
    item: PartnerConnectionWithUser,
    onCancel: () -> Unit,
    bgInner: Color,
    borderCol: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgInner,
        border = BorderStroke(1.dp, borderCol),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = NawanoAmber.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = "Pending",
                        tint = NawanoAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.partnerUser.fullName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.partnerUser.uniqueCode,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NawanoPrimary200,
                        maxLines = 1
                    )
                    Text(text = " • Pending", fontSize = 11.sp, color = NawanoAmber, maxLines = 1)
                }
            }

            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel Request",
                    tint = textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ConnectedPartnerCard(
    item: PartnerConnectionWithUser,
    onDisconnect: () -> Unit,
    bgInner: Color,
    borderCol: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgInner,
        border = BorderStroke(1.dp, borderCol),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = NawanoEmerald.copy(alpha = 0.15f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = item.partnerUser.fullName.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = NawanoEmerald
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.partnerUser.fullName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = textPrimary,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = NawanoEmerald.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Connected",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NawanoEmerald,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            maxLines = 1
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.partnerUser.uniqueCode,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NawanoPrimary200,
                        maxLines = 1
                    )
                    if (item.partnerUser.phoneNumber.isNotBlank()) {
                        Text(text = " • ", color = textSecondary)
                        Text(
                            text = item.partnerUser.phoneNumber,
                            fontSize = 11.sp,
                            color = textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            IconButton(onClick = onDisconnect) {
                Icon(
                    imageVector = Icons.Default.LinkOff,
                    contentDescription = "Disconnect Partner",
                    tint = NawanoRose,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyStateView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    textPrimary: Color,
    textSecondary: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = NawanoPrimary50,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NawanoPrimary200,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(16.dp))

        FilledTonalButton(
            onClick = onButtonClick,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = NawanoPrimary200,
                contentColor = Color.White
            )
        ) {
            Text(text = buttonText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}
