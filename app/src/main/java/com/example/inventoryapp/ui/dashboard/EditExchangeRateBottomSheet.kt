package com.example.inventoryapp.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

/**
 * Material 3 Swipe-Up Modal Bottom Sheet for configuring the fixed USD to KHR exchange rate.
 * Complies with the project architecture rule requiring ModalBottomSheet for user input and forms.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExchangeRateBottomSheet(
    currentRate: Double,
    isDarkTheme: Boolean,
    onSaveRate: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var rateInputText by remember {
        mutableStateOf(if (currentRate % 1.0 == 0.0) currentRate.toLong().toString() else currentRate.toString())
    }
    var errorText by remember { mutableStateOf<String?>(null) }

    val parsedRate = rateInputText.toDoubleOrNull()
    val isValid = parsedRate != null && parsedRate > 0.0

    val cardBg = if (isDarkTheme) NawanoDarkCard else Color(0xFFF8FAFC)
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    val quickPresets = listOf(4000.0, 4050.0, 4100.0, 4150.0, 4200.0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = if (isDarkTheme) Color(0xFF475569) else Color(0xFFCBD5E1)
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = if (isDarkTheme) NawanoDark200 else Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = NawanoPrimary200.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CurrencyExchange,
                            contentDescription = null,
                            tint = NawanoPrimary200,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Exchange Rate Setting",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )
                    Text(
                        text = "Configure 1 USD to Cambodian Riel (KHR) rate",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Rate Input Field
            OutlinedTextField(
                value = rateInputText,
                onValueChange = {
                    rateInputText = it.filter { ch -> ch.isDigit() || ch == '.' }
                    val p = rateInputText.toDoubleOrNull()
                    errorText = when {
                        rateInputText.isBlank() -> "Rate cannot be empty"
                        p == null || p <= 0.0 -> "Rate must be greater than 0"
                        else -> null
                    }
                },
                label = { Text("1 USD Exchange Rate") },
                prefix = {
                    Text("1 USD = ", fontWeight = FontWeight.Bold, color = NawanoPrimary200)
                },
                suffix = {
                    Text("KHR (៛)", fontWeight = FontWeight.SemiBold, color = textSecondary)
                },
                isError = errorText != null,
                supportingText = {
                    if (errorText != null) {
                        Text(text = errorText!!, color = MaterialTheme.colorScheme.error)
                    } else {
                        val displayRate = parsedRate ?: currentRate
                        Text(
                            text = "Currently set to 1 USD = ${NumberFormat.getNumberInstance(Locale.US).format(displayRate.toLong())} ៛",
                            color = textSecondary
                        )
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NawanoPrimary200,
                    unfocusedBorderColor = cardBorder,
                    focusedLabelColor = NawanoPrimary200
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Preset Chips
            Text(
                text = "Quick Presets",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                quickPresets.forEach { preset ->
                    val isSelected = parsedRate == preset
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) NawanoPrimary200 else cardBg,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) NawanoPrimary200 else cardBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                rateInputText = if (preset % 1.0 == 0.0) preset.toLong().toString() else preset.toString()
                                errorText = null
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = NumberFormat.getNumberInstance(Locale.US).format(preset.toLong()),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isSelected) Color.White else textPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Live Conversion Preview Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Live Conversion Preview",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val activeRate = if (isValid) parsedRate!! else currentRate

                    val samples = listOf(1.0, 5.0, 20.0, 100.0)
                    samples.forEachIndexed { index, usd ->
                        val khr = (usd * activeRate).toLong()
                        val khrFormatted = NumberFormat.getNumberInstance(Locale.US).format(khr)
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", usd)} USD",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary
                            )
                            Text(
                                text = "= $khrFormatted ៛",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = NawanoPrimary200
                            )
                        }
                        if (index < samples.size - 1) {
                            HorizontalDivider(
                                color = cardBorder.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        rateInputText = "4000"
                        errorText = null
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = textPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Reset 4,000",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }

                Button(
                    onClick = {
                        if (isValid && parsedRate != null) {
                            onSaveRate(parsedRate)
                            onDismiss()
                        }
                    },
                    enabled = isValid,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NawanoPrimary200,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save Rate",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
