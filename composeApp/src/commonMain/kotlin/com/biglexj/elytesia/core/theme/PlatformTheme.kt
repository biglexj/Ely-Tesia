package com.biglexj.elytesia.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
expect fun rememberPlatformColorScheme(enabled: Boolean, darkTheme: Boolean): ColorScheme?

expect fun isPlatformDynamicColorAvailable(): Boolean
