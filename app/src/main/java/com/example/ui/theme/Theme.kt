package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.util.AppVisualTheme

private fun colorSchemeForVisualTheme(theme: AppVisualTheme): ColorScheme {
    val primary = Color(theme.primaryHex)
    val secondary = Color(theme.secondaryHex)
    val bg = Color(theme.backgroundHex)
    val surf = Color(theme.surfaceHex)
    val surfVar = Color(theme.surfaceVariantHex)

    return if (theme.isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color(0xFF042F2E),
            primaryContainer = Color(0xFF1E3A8A),
            onPrimaryContainer = Color(0xFFE0F2FE),
            secondary = secondary,
            onSecondary = Color(0xFF022C22),
            secondaryContainer = Color(0xFF064E3B),
            onSecondaryContainer = Color(0xFFD1FAE5),
            tertiary = Color(0xFFF59E0B),
            onTertiary = Color(0xFF451A03),
            tertiaryContainer = Color(0xFF78350F),
            onTertiaryContainer = Color(0xFFFEF3C7),
            background = bg,
            surface = surf,
            surfaceVariant = surfVar,
            outline = Color(0xFF64748B),
            onBackground = Color(0xFFF1F5F9),
            onSurface = Color(0xFFF8FAFC),
            onSurfaceVariant = Color(0xFFCBD5E1)
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = surfVar,
            onPrimaryContainer = Color(theme.headerStartHex),
            secondary = secondary,
            onSecondary = Color.White,
            secondaryContainer = surfVar.copy(alpha = 0.7f),
            onSecondaryContainer = Color(theme.headerStartHex),
            tertiary = CyanTertiary,
            onTertiary = CyanOnTertiary,
            tertiaryContainer = CyanTertiaryContainer,
            onTertiaryContainer = CyanOnTertiaryContainer,
            background = bg,
            surface = surf,
            surfaceVariant = surfVar.copy(alpha = 0.45f),
            outline = LightOutline,
            onBackground = Color(0xFF0F172A),
            onSurface = Color(0xFF0F172A),
            onSurfaceVariant = Color(0xFF334155)
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    visualTheme: AppVisualTheme = AppVisualTheme.CYBER_DARK,
    content: @Composable () -> Unit,
) {
    val colorScheme = colorSchemeForVisualTheme(visualTheme)

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
