package com.budgetmanager.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Dynamic (per-device) colour is intentionally not used: status colours (blue/amber/red)
// must mean the same thing on every phone, so the palette is fixed rather than derived
// from wallpaper.
private val LightColorScheme = lightColorScheme(
    primary = AccentBlue,
    secondary = WarningAmber,
    tertiary = AcceptedGreen,
    error = OverBudgetRed,
    background = LightBackground,
    surface = LightSurface,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    secondary = WarningAmber,
    tertiary = AcceptedGreen,
    error = OverBudgetRed,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface
)

@Composable
fun BudgetManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
