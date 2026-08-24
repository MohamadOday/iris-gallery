package com.iris.gallery.ui.theme

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
import com.iris.gallery.data.AccentColor
import com.iris.gallery.data.ThemeMode

private fun createLightPalette(primary: Color, secondary: Color, tertiary: Color) = lightColorScheme(
    primary = primary,
    onPrimary = Color.White,
    primaryContainer = primary.copy(alpha = 0.15f),
    onPrimaryContainer = primary,
    secondary = secondary,
    onSecondary = Color.White,
    secondaryContainer = secondary.copy(alpha = 0.15f),
    onSecondaryContainer = secondary,
    tertiary = tertiary,
    background = Color(0xFFFBF8FE),
    surface = Color(0xFFFBF8FE),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurface = Color(0xFF1D1B20),
    onSurfaceVariant = Color(0xFF49454E),
)

private fun createDarkPalette(primary: Color, secondary: Color, tertiary: Color, amoled: Boolean) = darkColorScheme(
    primary = primary,
    onPrimary = Color(0xFF381E72),
    primaryContainer = primary.copy(alpha = 0.25f),
    onPrimaryContainer = primary,
    secondary = secondary,
    onSecondary = Color(0xFF332D41),
    secondaryContainer = secondary.copy(alpha = 0.25f),
    onSecondaryContainer = secondary,
    tertiary = tertiary,
    background = if (amoled) Color.Black else Color(0xFF141218),
    surface = if (amoled) Color.Black else Color(0xFF141218),
    surfaceVariant = if (amoled) Color(0xFF161616) else Color(0xFF49454E),
    onSurface = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFFCAC4D0),
)

@Composable
fun IrisTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    amoledBlack: Boolean = false,
    accentColor: AccentColor = AccentColor.IRIS,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val colors = if (accentColor == AccentColor.MATERIAL_YOU && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val dynamicScheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        if (isDark && amoledBlack) {
            dynamicScheme.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceVariant = Color(0xFF141414)
            )
        } else dynamicScheme
    } else {
        val (primary, secondary, tertiary) = when (accentColor) {
            AccentColor.IRIS -> Triple(Color(0xFF8A4AF3), Color(0xFF6C5CE7), Color(0xFFD0BCFF))
            AccentColor.LAPIS_MESOPOTAMIA -> Triple(Color(0xFF1976D2), Color(0xFF0288D1), Color(0xFFFFD700))
            AccentColor.EMERALD -> Triple(Color(0xFF00897B), Color(0xFF26A69A), Color(0xFF80CBC4))
            AccentColor.ISHTAR_AMBER -> Triple(Color(0xFFF57C00), Color(0xFFFFA726), Color(0xFFFFD54F))
            AccentColor.ROSE -> Triple(Color(0xFFD81B60), Color(0xFFEC407A), Color(0xFFFF80AB))
            AccentColor.MATERIAL_YOU -> Triple(Color(0xFF8A4AF3), Color(0xFF6C5CE7), Color(0xFFD0BCFF))
        }

        if (isDark) {
            createDarkPalette(primary, secondary, tertiary, amoledBlack)
        } else {
            createLightPalette(primary, secondary, tertiary)
        }
    }

    MaterialTheme(colorScheme = colors, content = content)
}
