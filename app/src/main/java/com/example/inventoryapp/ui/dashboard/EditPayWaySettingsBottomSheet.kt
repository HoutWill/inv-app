package com.example.inventoryapp.ui.dashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.remote.payway.AbaPaywayService
import com.example.inventoryapp.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPayWaySettingsBottomSheet(
    currentMerchantId: String,
    currentApiKey: String,
    currentIsSandbox: Boolean,
    isDarkTheme: Boolean,
    onSaveConfig: (merchantId: String, apiKey: String, isSandbox: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberSafeModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    var merchantIdInput by remember { mutableStateOf(currentMerchantId.ifBlank { AbaPaywayService.DEFAULT_SANDBOX_MERCHANT_ID }) }
    var apiKeyInput by remember { mutableStateOf(currentApiKey) }
    var isSandbox by remember { mutableStateOf(currentIsSandbox) }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var testResultStatus by remember { mutableStateOf<String?>(null) }
    var isTestSuccess by remember { mutableStateOf(false) }

    val bgSurface = if (isDarkTheme) Color(0xFF191D2D) else Color.White
    val cardBg = if (isDarkTheme) Color(0xFF22263C) else Color(0xFFF8FAFC)
    val cardBorder = if (isDarkTheme) Color(0xFF2E334D) else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDarkTheme) Color(0xFF8E95AF) else Color(0xFF64748B)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                width = 44.dp,
                height = 5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
        },
        containerColor = bgSurface,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // Header: ABA PayWay Badge + Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ABA PayWay circular emblem
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF005E7B))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(0.6f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ABA",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(0.4f)
                                .fillMaxWidth()
                                .background(Color(0xFFEB2227)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "PAYWAY",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ABA PayWay Gateway",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "Configure merchant credentials for KHQR & In-App payments",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = cardBorder)
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Environment Toggle: Sandbox vs Production
            Text(
                text = "ENVIRONMENT MODE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = cardBg,
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSandbox) "Sandbox Mode (Testing)" else "Production Mode (Live)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSandbox) NawanoPrimary200 else Color(0xFF10B981)
                        )
                        Text(
                            text = if (isSandbox) "checkout-sandbox.payway.com.kh" else "checkout.payway.com.kh",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = textSecondary
                        )
                    }

                    Switch(
                        checked = !isSandbox,
                        onCheckedChange = { isSandbox = !it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF10B981),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = NawanoPrimary200
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Merchant ID Input
            Text(
                text = "MERCHANT ID",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = merchantIdInput,
                onValueChange = { merchantIdInput = it.trim() },
                placeholder = { Text("e.g. ec000002", color = textSecondary.copy(alpha = 0.5f)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = null,
                        tint = NawanoPrimary200,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (merchantIdInput.isNotBlank()) {
                        IconButton(onClick = { merchantIdInput = "" }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NawanoPrimary200,
                    unfocusedBorderColor = cardBorder,
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. API Secret Key Input
            Text(
                text = "API SECRET KEY (HMAC-SHA512)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it.trim() },
                placeholder = { Text("Enter your PayWay API secret key", color = textSecondary.copy(alpha = 0.5f)) },
                visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = NawanoPrimary200,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                        Icon(
                            imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Visibility",
                            tint = textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NawanoPrimary200,
                    unfocusedBorderColor = cardBorder,
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick preset helpers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reset Sandbox Default",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NawanoPrimary200,
                    modifier = Modifier.clickable {
                        merchantIdInput = AbaPaywayService.DEFAULT_SANDBOX_MERCHANT_ID
                        isSandbox = true
                    }
                )

                // Link to developer.payway.com.kh
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://developer.payway.com.kh/"))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                ) {
                    Text(
                        text = "PayWay Portal",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textSecondary
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            // Test Connection Feedback Banner
            if (testResultStatus != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isTestSuccess) Color(0xFF10B981).copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, if (isTestSuccess) Color(0xFF10B981).copy(alpha = 0.4f) else MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isTestSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = if (isTestSuccess) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = testResultStatus ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (isTestSuccess) Color(0xFF047857) else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Test Connection Button
                OutlinedButton(
                    onClick = {
                        isTestingConnection = true
                        testResultStatus = null
                        coroutineScope.launch {
                            val res = AbaPaywayService.generateKhqr(
                                merchantId = merchantIdInput,
                                apiKey = apiKeyInput,
                                isSandbox = isSandbox,
                                amount = 1.0,
                                currency = "USD",
                                itemsSummary = "PayWay API Test"
                            )
                            isTestingConnection = false
                            if (res.isLiveApi) {
                                isTestSuccess = true
                                testResultStatus = "Success! Connected to PayWay ($merchantIdInput)."
                            } else if (apiKeyInput.isBlank()) {
                                isTestSuccess = true
                                testResultStatus = "Offline Simulation Mode (Ready for testing without live key)."
                            } else {
                                isTestSuccess = false
                                testResultStatus = "API test returned: ${res.errorMessage ?: "Check merchant ID / secret key"}"
                            }
                        }
                    },
                    enabled = !isTestingConnection,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier
                        .weight(0.42f)
                        .height(48.dp)
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = "Test API",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary
                        )
                    }
                }

                // Save Configuration Button
                Button(
                    onClick = {
                        onSaveConfig(merchantIdInput, apiKeyInput, isSandbox)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200),
                    modifier = Modifier
                        .weight(0.58f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save Credentials",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
