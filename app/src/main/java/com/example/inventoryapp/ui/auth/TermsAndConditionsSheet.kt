package com.example.inventoryapp.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.ui.theme.NawanoLightOutline
import com.example.inventoryapp.ui.theme.NawanoLightSurface
import com.example.inventoryapp.ui.theme.NawanoPrimary50
import com.example.inventoryapp.ui.theme.NawanoPrimary200
import com.example.inventoryapp.ui.theme.NawanoTextPrimaryLight
import com.example.inventoryapp.ui.theme.NawanoTextSecondaryLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndConditionsSheet(
    onDismiss: () -> Unit,
    onAccept: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NawanoLightSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = NawanoPrimary50,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = NawanoPrimary200,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp)
                        )
                    }
                    Text(
                        text = "Terms & Operating Rules",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NawanoTextPrimaryLight,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = NawanoTextSecondaryLight
                    )
                }
            }

            HorizontalDivider(
                color = NawanoLightOutline,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .height(420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                TermsSection(
                    number = "1",
                    title = "Role-Based Access & Scopes",
                    body = "• Business Owners maintain full access to product management, pricing models, stock counts, and supplier catalogs.\n• Supplier Partners are restricted exclusively to their own assigned purchase orders. Retail profit margins and third-party supplier listings are strictly confidential."
                )

                TermsSection(
                    number = "2",
                    title = "Purchase Order & Inbound Rules",
                    body = "• Orders are issued in PENDING status. The Owner can cancel an order anytime before confirmation.\n• Suppliers may CONFIRM or REJECT with a valid reason.\n• Critical Stock Rule: Marking an order as DELIVERED does not increase warehouse inventory. Stock on hand is only updated when the Owner inspects and marks the order as RECEIVED."
                )

                TermsSection(
                    number = "3",
                    title = "Inventory Accuracy & Audit Trails",
                    body = "• Every stock movement (Inbound, Outbound, Restock, Shrinkage/Loss) writes an unalterable audit log with timestamp, operator ID, and previous/new quantities."
                )

                TermsSection(
                    number = "4",
                    title = "Pricing & Data Confidentiality",
                    body = "• Unit purchase costs, volume tier discounts, and supplier contacts are confidential commercial data.\n• All local passwords are encrypted with PBKDF2 salted hashes. Plaintext credentials are never stored."
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onAccept,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NawanoPrimary200,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "I Understand & Agree",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TermsSection(
    number: String,
    title: String,
    body: String
) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = "$number. $title",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = NawanoTextPrimaryLight
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = body,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = NawanoTextSecondaryLight
        )
    }
}
