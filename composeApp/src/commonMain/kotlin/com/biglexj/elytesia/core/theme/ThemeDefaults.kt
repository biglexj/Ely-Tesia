package com.biglexj.elytesia.theme

object ThemeDefaults {
    val Aurora = ElyThemeDefinition(
        id = "com.biglexj.aurora",
        name = "Aurora",
        author = "biglexj",
        description = "Tema original oscuro de Ely-Tesia.",
        builtIn = true,
        material = MaterialThemeTokens(
            primary = "#5B4CFF", onPrimary = "#FFFFFF",
            primaryContainer = "#302A78", onPrimaryContainer = "#E5E1FF",
            secondary = "#00C7B1", onSecondary = "#001F1B",
            secondaryContainer = "#004F47", onSecondaryContainer = "#A5F2E8",
            tertiary = "#FB7793", onTertiary = "#3B0715",
            background = "#0F172A", onBackground = "#F8FAFC",
            surface = "#1E293B", onSurface = "#F8FAFC",
            surfaceVariant = "#334155", onSurfaceVariant = "#CBD5E1",
            error = "#FFD166", onError = "#2C1C00", outline = "#64748B"
        ),
        music = MusicThemeTokens(
            leftHand = "#00C7B1", rightHand = "#5B4CFF", neutralTrack = "#FFE6CA",
            whiteKey = "#E2E8F0", whiteKeyPressed = "#00C7B1",
            blackKey = "#0F172A", blackKeyPressed = "#FB7793",
            correctNote = "#00C7B1", wrongNote = "#FFD166", waitingNote = "#FB7793",
            particleLeft = "#00C7B1CC", particleRight = "#5B4CFFCC"
        )
    )

    val Classic = ElyThemeDefinition(
        id = "com.biglexj.classic",
        name = "Clásico",
        author = "biglexj",
        description = "Marfil, granate y azul de concierto.",
        builtIn = true,
        material = MaterialThemeTokens(
            primary = "#A43D52", onPrimary = "#FFFFFF",
            primaryContainer = "#5A1727", onPrimaryContainer = "#FFD9DF",
            secondary = "#3E6C8C", onSecondary = "#FFFFFF",
            secondaryContainer = "#18364A", onSecondaryContainer = "#C9E6FF",
            tertiary = "#D5A021", onTertiary = "#261A00",
            background = "#171411", onBackground = "#F5EEE4",
            surface = "#24201C", onSurface = "#F5EEE4",
            surfaceVariant = "#3A342E", onSurfaceVariant = "#D8CEC3",
            error = "#FFB4AB", onError = "#690005", outline = "#8E8378"
        ),
        music = MusicThemeTokens(
            leftHand = "#3E8EAF", rightHand = "#B8485F", neutralTrack = "#D5A021",
            whiteKey = "#F1E5D2", whiteKeyPressed = "#3E8EAF",
            blackKey = "#191512", blackKeyPressed = "#B8485F",
            correctNote = "#70B77E", wrongNote = "#FFB347", waitingNote = "#D5A021",
            particleLeft = "#3E8EAFCC", particleRight = "#B8485FCC"
        )
    )

    val HighContrast = ElyThemeDefinition(
        id = "com.biglexj.high-contrast",
        name = "Alto Contraste",
        author = "biglexj",
        description = "Contraste reforzado para accesibilidad.",
        builtIn = true,
        material = MaterialThemeTokens(
            primary = "#FFFF00", onPrimary = "#000000",
            primaryContainer = "#4A4A00", onPrimaryContainer = "#FFFFFF",
            secondary = "#00FFFF", onSecondary = "#000000",
            secondaryContainer = "#004A4A", onSecondaryContainer = "#FFFFFF",
            tertiary = "#FF66FF", onTertiary = "#000000",
            background = "#000000", onBackground = "#FFFFFF",
            surface = "#0A0A0A", onSurface = "#FFFFFF",
            surfaceVariant = "#242424", onSurfaceVariant = "#FFFFFF",
            error = "#FF6B6B", onError = "#000000", outline = "#FFFFFF"
        ),
        music = MusicThemeTokens(
            leftHand = "#00FFFF", rightHand = "#FFFF00", neutralTrack = "#FF66FF",
            whiteKey = "#FFFFFF", whiteKeyPressed = "#00FFFF",
            blackKey = "#000000", blackKeyPressed = "#FFFF00",
            correctNote = "#00FF66", wrongNote = "#FF3B30", waitingNote = "#FF66FF",
            particleLeft = "#00FFFF", particleRight = "#FFFF00"
        ),
        effects = ThemeEffects(pressedGlow = 1f, noteTrail = 0.8f, particleIntensity = 0.65f)
    )

    val builtIns = listOf(Aurora, Classic, HighContrast)
    fun byId(id: String?): ElyThemeDefinition = builtIns.firstOrNull { it.id == id } ?: Aurora
}
