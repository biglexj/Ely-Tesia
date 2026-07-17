package com.biglexj.elytesia.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
actual fun rememberPlatformColorScheme(enabled: Boolean, darkTheme: Boolean): ColorScheme? = null

actual fun isPlatformDynamicColorAvailable(): Boolean = false
