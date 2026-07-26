package com.biglexj.elytesia.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
actual fun rememberPlatformColorScheme(enabled: Boolean, darkTheme: Boolean): ColorScheme? {
    if (!enabled) return null
    return DynamicThemeEngine.createSeedColorScheme(
        seedColor = Color(0xFF5B4CFF),
        darkTheme = darkTheme
    )
}

actual fun isPlatformDynamicColorAvailable(): Boolean = true
