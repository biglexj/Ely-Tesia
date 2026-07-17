package com.biglexj.elytesia.theme

import com.biglexj.elytesia.storage.AppStateCodec
import com.biglexj.elytesia.storage.SavedAppState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThemeJsonCodecTest {
    @Test
    fun builtInThemesRoundTripThroughJson() {
        ThemeDefaults.builtIns.forEach { original ->
            assertTrue(ThemeValidator.validate(original).isEmpty(), "${original.name} debe ser legible")
            val decoded = ThemeJsonCodec.decode(ThemeJsonCodec.encode(original)).getOrThrow()
            assertEquals(original.copy(builtIn = false), decoded)
        }
    }

    @Test
    fun validatorRejectsUnknownSchemaAndInvalidColor() {
        val invalid = ThemeDefaults.Aurora.copy(
            schemaVersion = 2,
            material = ThemeDefaults.Aurora.material.copy(primary = "violeta")
        )
        val errors = ThemeValidator.validate(invalid)
        assertTrue(errors.any { it.contains("schemaVersion") })
        assertTrue(errors.any { it.contains("Color inválido") })
    }

    @Test
    fun appStateKeepsSelectedAndImportedTheme() {
        val json = ThemeJsonCodec.encode(ThemeDefaults.Classic.copy(id = "com.community.classic"))
        val state = SavedAppState(selectedThemeId = "com.community.classic", useDynamicColor = true, importedThemes = listOf(json))
        val restored = AppStateCodec.decode(AppStateCodec.encode(state))!!
        assertEquals(state.selectedThemeId, restored.selectedThemeId)
        assertEquals(state.useDynamicColor, restored.useDynamicColor)
        assertEquals(state.importedThemes, restored.importedThemes)
    }
}
