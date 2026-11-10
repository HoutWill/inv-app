package com.example.inventoryapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NawanoPrimary200,
    onPrimary = Color.White,
    primaryContainer = NawanoPrimary200.copy(alpha = 0.22f),
    onPrimaryContainer = NawanoPrimary100,
    secondary = NawanoGradientEnd,
    onSecondary = Color.White,
    secondaryContainer = NawanoDarkCard,
    onSecondaryContainer = NawanoSecondary100,
    tertiary = NawanoEmerald,
    onTertiary = Color.White,
    error = NawanoRose,
    onError = Color.White,
    errorContainer = Color(0xFF4C1D24),
    onErrorContainer = Color(0xFFFFCDD2),
    background = NawanoDark400,
    onBackground = NawanoTextPrimaryDark,
    surface = NawanoDark200,
    onSurface = NawanoTextPrimaryDark,
    surfaceVariant = NawanoDarkCard,
    onSurfaceVariant = NawanoTextSecondaryDark,
    surfaceContainer = NawanoDark200,
    surfaceContainerHigh = NawanoDarkCard,
    surfaceContainerHighest = Color(0xFF2A2D5E),
    outline = NawanoDarkOutline,
    outlineVariant = Color(0xFF252857)
)

private val LightColorScheme = lightColorScheme(
    primary = NawanoPrimary200,
    onPrimary = Color.White,
    primaryContainer = NawanoPrimary50,
    onPrimaryContainer = NawanoPrimary200,
    secondary = NawanoGradientEnd,
    onSecondary = Color.White,
    secondaryContainer = NawanoSecondary100,
    onSecondaryContainer = Color(0xFF5B21B6),
    tertiary = NawanoEmerald,
    onTertiary = Color.White,
    error = NawanoRose,
    onError = Color.White,
    errorContainer = Color(0xFFFDE8E8),
    onErrorContainer = Color(0xFF991B1B),
    background = NawanoNeutral50,
    onBackground = NawanoTextPrimaryLight,
    surface = NawanoLightSurface,
    onSurface = NawanoTextPrimaryLight,
    surfaceVariant = NawanoPrimary50,
    onSurfaceVariant = NawanoTextSecondaryLight,
    surfaceContainer = NawanoLightSurface,
    surfaceContainerHigh = NawanoNeutral50,
    surfaceContainerHighest = Color(0xFFEDEDF7),
    outline = NawanoLightOutline,
    outlineVariant = Color(0xFFE2E4F0)
)

@Composable
fun InventoryAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false so the signature Blue & White brand palette takes priority
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
