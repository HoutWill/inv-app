package com.example.inventoryapp.ui.partner

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.UserEntity
import com.example.inventoryapp.data.local.entity.UserRole
import com.example.inventoryapp.ui.dashboard.PartnerConnectionUiState
import com.example.inventoryapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectPartnerBottomSheet(
    currentUser: UserEntity,
    partnerUiState: PartnerConnectionUiState,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSendRequest: (target: UserEntity, note: String) -> Unit,
    onOpenRequests: () -> Unit,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean = false,
    pendingRequestsCount: Int = 0
) {
    val sheetState = rememberSafeModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf("") }
    var inviteNote by remember { mutableStateOf("") }

    val bgCard = if (isDarkTheme) NawanoDark200 else Color.White
    val bgInner = if (isDarkTheme) NawanoDark400 else NawanoNeutral50
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight
    val borderCol = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)

    val targetRoleName = if (currentUser.role == UserRole.OWNER) "Supplier" else "Middle Man"
    val myRoleName = if (currentUser.role == UserRole.OWNER) "Middle Man" else "Supplier"

    LaunchedEffect(Unit) {
        onClearSearch()
    }

    ModalBottomSheet(
        onDismissRequest = {
            onClearSearch()
            onDismiss()
        },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = bgCard,
        dragHandle = { BottomSheetDefaults.DragHandle(color = textSecondary.copy(alpha = 0.4f)) },
        modifier = Modifier.navigationBarsPadding().imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header Row - Responsive for small screens
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "Add $targetRoleName",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Connect by Unique ID or Phone",
                        fontSize = 12.sp,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (pendingRequestsCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = {
                            onDismiss()
                            onOpenRequests()
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = NawanoPrimary50,
                            contentColor = NawanoPrimary200
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mail,
                            contentDescription = "Requests",
                            modifier = Modifier.size(15.dp),
                            tint = NawanoPrimary200
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "$pendingRequestsCount Requests",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // My Identity & Share Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = bgInner,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, borderCol)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = NawanoPrimary200,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = currentUser.fullName.take(2).uppercase().ifBlank { "ME" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentUser.fullName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = textPrimary,
                                    modifier = Modifier.weight(1f, fill = false),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = NawanoPrimary200.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = myRoleName,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NawanoPrimary200,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Share your details so $targetRoleName can add you:",
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = borderCol.copy(alpha = 0.6f))
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Copy ID button
                        Surface(
                            onClick = {
                                val code = currentUser.uniqueCode.ifBlank { "MID-1001" }
                                clipboardManager.setText(AnnotatedString(code))
                                Toast.makeText(context, "Copied ID: $code", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = bgCard,
                            border = BorderStroke(1.dp, borderCol),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Copy ID",
                                    modifier = Modifier.size(14.dp),
                                    tint = NawanoPrimary200
                                )
                                Spacer(Modifier.width(4.dp))
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = "Unique ID",
                                        fontSize = 9.5.sp,
                                        color = textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = currentUser.uniqueCode.ifBlank { "MID-1001" },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = NawanoPrimary200,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Copy Phone button
                        val phoneDisplay = currentUser.phoneNumber.ifBlank { "+1 (555) 019-2831" }
                        Surface(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(phoneDisplay))
                                Toast.makeText(context, "Copied Phone: $phoneDisplay", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = bgCard,
                            border = BorderStroke(1.dp, borderCol),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Copy Phone",
                                    modifier = Modifier.size(14.dp),
                                    tint = NawanoPrimary200
                                )
                                Spacer(Modifier.width(4.dp))
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = "Phone Number",
                                        fontSize = 9.5.sp,
                                        color = textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = phoneDisplay,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Search Bar Section
            Text(
                text = "Search $targetRoleName",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    if (partnerUiState.searchResult != null || partnerUiState.searchError != null) {
                        onClearSearch()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = if (currentUser.role == UserRole.OWNER) "Enter SUP-XXXX or phone..." else "Enter MID-XXXX or phone...",
                        fontSize = 14.sp,
                        color = textSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (searchQuery.isNotBlank()) NawanoPrimary200 else textSecondary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            onClearSearch()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = textSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NawanoPrimary200,
                    unfocusedBorderColor = borderCol,
                    focusedContainerColor = bgCard,
                    unfocusedContainerColor = bgCard,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    if (searchQuery.isNotBlank()) onSearch(searchQuery)
                })
            )

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    if (searchQuery.isNotBlank()) onSearch(searchQuery)
                },
                enabled = searchQuery.isNotBlank() && !partnerUiState.isSearching,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NawanoPrimary200,
                    contentColor = Color.White
                )
            ) {
                if (partnerUiState.isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(text = "Find $targetRoleName", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            // Error feedback
            partnerUiState.searchError?.let { err ->
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NawanoRose.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, NawanoRose.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = NawanoRose, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(text = err, color = NawanoRose, fontSize = 12.sp)
                    }
                }
            }

            // Action success feedback
            partnerUiState.actionMessage?.let { msg ->
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NawanoEmerald.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, NawanoEmerald.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = NawanoEmerald, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(text = msg, color = NawanoEmerald, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Matched Partner Preview Card
            partnerUiState.searchResult?.let { partner ->
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "Matched Partner Found",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NawanoEmerald
                )
                Spacer(Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = bgInner,
                    border = BorderStroke(1.5.dp, NawanoPrimary200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = NawanoPrimary200,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = partner.fullName.take(2).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = partner.fullName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = partner.uniqueCode,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = NawanoPrimary200,
                                        maxLines = 1
                                    )
                                    if (partner.phoneNumber.isNotBlank()) {
                                        Text(text = " • ", color = textSecondary)
                                        Text(
                                            text = partner.phoneNumber,
                                            fontSize = 11.sp,
                                            color = textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        OutlinedTextField(
                            value = inviteNote,
                            onValueChange = { inviteNote = it },
                            placeholder = {
                                Text(
                                    text = "Optional note (e.g. Hi, let's partner up!)",
                                    fontSize = 13.sp,
                                    color = textSecondary
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NawanoPrimary200,
                                unfocusedBorderColor = borderCol,
                                focusedContainerColor = bgCard,
                                unfocusedContainerColor = bgCard,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary
                            )
                        )

                        Spacer(Modifier.height(14.dp))

                        Button(
                            onClick = {
                                onSendRequest(partner, inviteNote)
                            },
                            enabled = !partnerUiState.isSubmitting,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NawanoPrimary200,
                                contentColor = Color.White
                            )
                        ) {
                            if (partnerUiState.isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(text = "Send Connection Request", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Quick view requests button at bottom
            OutlinedButton(
                onClick = {
                    onDismiss()
                    onOpenRequests()
                },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, borderCol),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary)
            ) {
                Icon(imageVector = Icons.Outlined.People, contentDescription = null, modifier = Modifier.size(18.dp), tint = NawanoPrimary200)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (pendingRequestsCount > 0) "Manage Connections ($pendingRequestsCount Pending)" else "Manage Connections & Requests",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
