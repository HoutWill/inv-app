package com.example.inventoryapp.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.inventoryapp.R
import com.example.inventoryapp.ui.theme.NawanoGradientEnd
import com.example.inventoryapp.ui.theme.NawanoGradientStart
import com.example.inventoryapp.ui.theme.NawanoPrimary200

/**
 * Modern Floating Bottom Navigation Bar with elevated pop-up scan button.
 *
 * @param selectedTab Index of currently selected tab (0: Home, 1: Items, 2: Stats, 3: Settings).
 * @param onTabSelected Callback invoked when a tab is selected.
 * @param onScanClicked Callback invoked when the central elevated scanner FAB is clicked.
 * @param totalItemCount Total number of inventory items (optional badge counter).
 */
@Composable
fun InventoryBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onScanClicked: () -> Unit,
    totalItemCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(top = 18.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, NawanoPrimary200.copy(alpha = 0.20f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 0: Home
                FloatingNavItem(
                    title = "Home",
                    icon = if (selectedTab == 0) Icons.Default.Home else Icons.Outlined.Home,
                    isSelected = selectedTab == 0,
                    onClick = { onTabSelected(0) }
                )

                // Tab 1: Items (Vector icon from iconpacks.net)
                FloatingNavItem(
                    title = "Items",
                    painter = painterResource(R.drawable.ic_inventory_box),
                    isSelected = selectedTab == 1,
                    onClick = { onTabSelected(1) }
                )

                // Center placeholder space for the elevated pop-up scan button
                Spacer(modifier = Modifier.width(56.dp))

                // Tab 2: Stats
                FloatingNavItem(
                    title = "Stats",
                    icon = if (selectedTab == 2) Icons.Default.BarChart else Icons.Outlined.BarChart,
                    isSelected = selectedTab == 2,
                    onClick = { onTabSelected(2) }
                )

                // Tab 3: Settings
                FloatingNavItem(
                    title = "Settings",
                    icon = if (selectedTab == 3) Icons.Default.Settings else Icons.Outlined.Settings,
                    isSelected = selectedTab == 3,
                    onClick = { onTabSelected(3) }
                )
            }
        }

        // Center Popped-Up Floating Scan Button (Elevated above bar with Nawano gradient & white QR icon)
        FloatingScanButton(
            onClick = onScanClicked,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-18).dp)
        )
    }
}

/**
 * Backward-compatibility alias for [InventoryBottomBar].
 */
@Composable
fun FloatingInventoryBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onScanClicked: () -> Unit,
    totalItemCount: Int = 0,
    modifier: Modifier = Modifier
) = InventoryBottomBar(
    selectedTab = selectedTab,
    onTabSelected = onTabSelected,
    onScanClicked = onScanClicked,
    totalItemCount = totalItemCount,
    modifier = modifier
)

@Composable
fun FloatingNavItem(
    title: String,
    isSelected: Boolean,
    icon: ImageVector? = null,
    painter: Painter? = null,
    onClick: () -> Unit
) {
    val activeColor = NawanoPrimary200
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

    val animatedBgColor by animateColorAsState(
        targetValue = if (isSelected) activeColor.copy(alpha = 0.14f) else Color.Transparent,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "NavBgColor"
    )
    val animatedIconTint by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "NavIconTint"
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
        label = "NavScale"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                }
                .clip(CircleShape)
                .background(animatedBgColor),
            contentAlignment = Alignment.Center
        ) {
            if (painter != null) {
                Icon(
                    painter = painter,
                    contentDescription = title,
                    tint = animatedIconTint,
                    modifier = Modifier.size(22.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = animatedIconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = animatedIconTint
        )
    }
}

@Composable
fun FloatingScanButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(4.dp, MaterialTheme.colorScheme.surface),
        modifier = modifier.size(58.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(NawanoGradientStart, NawanoGradientEnd)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "Quick Scan Barcode",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
