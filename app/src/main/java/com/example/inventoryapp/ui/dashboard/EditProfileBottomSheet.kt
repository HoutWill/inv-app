package com.example.inventoryapp.ui.dashboard

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.UserEntity
import com.example.inventoryapp.data.local.entity.UserRole
import com.example.inventoryapp.ui.theme.*

/**
 * Swipe-up modal bottom sheet allowing the user to view and edit their profile details:
 * Full Name, Phone Number, and Unique Partner Code, while viewing read-only account email and role.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileBottomSheet(
    user: UserEntity,
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (fullName: String, phoneNumber: String, uniqueCode: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberSafeModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current

    var fullName by remember(user.fullName) { mutableStateOf(user.fullName) }
    var phoneNumber by remember(user.phoneNumber) { mutableStateOf(user.phoneNumber) }
    var uniqueCode by remember(user.uniqueCode) { mutableStateOf(user.uniqueCode) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var codeError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val bgCard = if (isDarkTheme) NawanoDark200 else Color.White
    val bgInner = if (isDarkTheme) NawanoDark400 else NawanoNeutral50
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight
    val borderCol = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)

    val dynamicInitials = remember(fullName) {
        fullName.trim().split("\\s+".toRegex())
            .mapNotNull { part -> part.firstOrNull { it.isLetter() }?.toString() }
            .take(2)
            .joinToString("")
            .uppercase()
            .ifEmpty { "NW" }
    }

    val roleLabel = when (user.role) {
        UserRole.OWNER -> "Middle Man • Nawano HQ"
        UserRole.SUPPLIER -> "Supplier • Verified Partner"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = bgCard,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = textSecondary.copy(alpha = 0.4f)
            )
        },
        modifier = modifier
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header Row: Avatar + Title + Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box {
                        Surface(
                            shape = CircleShape,
                            color = NawanoPrimary200,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = dynamicInitials,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    color = Color.White
                                )
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = bgCard,
                            border = BorderStroke(1.5.dp, NawanoPrimary200),
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.BottomEnd)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Avatar",
                                    tint = NawanoPrimary200,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }

                    Column {
                        Text(
                            text = "Edit Profile",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary
                        )
                        Text(
                            text = "Personal info & partner identification",
                            fontSize = 12.5.sp,
                            color = textSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Account Security / Read-Only Info Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = bgInner,
                border = BorderStroke(1.dp, borderCol),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Email,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = user.email.ifBlank { "middleman@nawano.com" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "Verified",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = borderCol.copy(alpha = 0.6f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Shield,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Role",
                                fontSize = 12.sp,
                                color = textSecondary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NawanoPrimary200.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, NawanoPrimary200.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = roleLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NawanoPrimary200,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 1. Full Name Input
            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    if (nameError != null && it.isNotBlank()) nameError = null
                },
                label = { Text("Full Name") },
                placeholder = { Text("e.g. Anthony Middleman") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = if (nameError != null) MaterialTheme.colorScheme.error else NawanoPrimary200
                    )
                },
                trailingIcon = {
                    if (fullName.isNotBlank()) {
                        IconButton(onClick = { fullName = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                isError = nameError != null,
                supportingText = {
                    nameError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NawanoPrimary200,
                    unfocusedBorderColor = borderCol,
                    focusedContainerColor = bgInner,
                    unfocusedContainerColor = bgInner,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            // 2. Phone Number Input
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number") },
                placeholder = { Text("+1 (555) 019-2831") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Phone,
                        contentDescription = null,
                        tint = NawanoPrimary200
                    )
                },
                trailingIcon = {
                    if (phoneNumber.isNotBlank()) {
                        IconButton(onClick = { phoneNumber = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NawanoPrimary200,
                    unfocusedBorderColor = borderCol,
                    focusedContainerColor = bgInner,
                    unfocusedContainerColor = bgInner,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            // 3. Unique Partner Code Input
            OutlinedTextField(
                value = uniqueCode,
                onValueChange = {
                    uniqueCode = it.uppercase()
                    if (codeError != null && it.isNotBlank()) codeError = null
                },
                label = { Text("Unique Partner Code") },
                placeholder = { Text("MID-1001") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Badge,
                        contentDescription = null,
                        tint = if (codeError != null) MaterialTheme.colorScheme.error else NawanoPrimary200
                    )
                },
                trailingIcon = {
                    if (uniqueCode.isNotBlank()) {
                        IconButton(onClick = { uniqueCode = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                isError = codeError != null,
                supportingText = {
                    if (codeError != null) {
                        Text(codeError!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    } else {
                        Text(
                            "Used by business partners to search and connect with your inventory.",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NawanoPrimary200,
                    unfocusedBorderColor = borderCol,
                    focusedContainerColor = bgInner,
                    unfocusedContainerColor = bgInner,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            // Action Buttons: Cancel + Save Changes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, borderCol),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = textPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        val cleanName = fullName.trim()
                        val cleanCode = uniqueCode.trim()

                        if (cleanName.isBlank()) {
                            nameError = "Full name cannot be empty"
                            return@Button
                        }
                        if (cleanCode.isBlank()) {
                            codeError = "Partner code cannot be empty"
                            return@Button
                        }

                        isSaving = true
                        onSave(cleanName, phoneNumber.trim(), cleanCode)
                    },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NawanoPrimary200,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(50.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Save Changes",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
