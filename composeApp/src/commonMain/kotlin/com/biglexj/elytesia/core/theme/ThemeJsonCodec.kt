package com.biglexj.elytesia.theme

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

object ThemeJsonCodec {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun decode(source: String): Result<ElyThemeDefinition> = runCatching {
        require(source.encodeToByteArray().size <= ThemeValidator.MAX_FILE_BYTES) { "El tema supera 64 KiB." }
        val root = json.parseToJsonElement(source).jsonObject
        val material = root.requiredObject("material")
        val music = root.requiredObject("music")
        val effects = root["effects"]?.jsonObject
        ElyThemeDefinition(
            schemaVersion = root.requiredInt("schemaVersion"),
            id = root.requiredString("id"),
            name = root.requiredString("name"),
            author = root.requiredString("author"),
            description = root.optionalString("description"),
            version = root.optionalString("version").ifBlank { "1.0.0" },
            license = root.optionalString("license").ifBlank { "CC0-1.0" },
            mode = root.optionalString("mode").uppercase().let { value ->
                ThemeMode.entries.firstOrNull { it.name == value } ?: ThemeMode.DARK
            },
            material = MaterialThemeTokens(
                primary = material.requiredString("primary"), onPrimary = material.requiredString("onPrimary"),
                primaryContainer = material.requiredString("primaryContainer"), onPrimaryContainer = material.requiredString("onPrimaryContainer"),
                secondary = material.requiredString("secondary"), onSecondary = material.requiredString("onSecondary"),
                secondaryContainer = material.requiredString("secondaryContainer"), onSecondaryContainer = material.requiredString("onSecondaryContainer"),
                tertiary = material.requiredString("tertiary"), onTertiary = material.requiredString("onTertiary"),
                background = material.requiredString("background"), onBackground = material.requiredString("onBackground"),
                surface = material.requiredString("surface"), onSurface = material.requiredString("onSurface"),
                surfaceVariant = material.requiredString("surfaceVariant"), onSurfaceVariant = material.requiredString("onSurfaceVariant"),
                error = material.requiredString("error"), onError = material.requiredString("onError"),
                outline = material.requiredString("outline")
            ),
            music = MusicThemeTokens(
                leftHand = music.requiredString("leftHand"), rightHand = music.requiredString("rightHand"),
                neutralTrack = music.requiredString("neutralTrack"), whiteKey = music.requiredString("whiteKey"),
                whiteKeyPressed = music.requiredString("whiteKeyPressed"), blackKey = music.requiredString("blackKey"),
                blackKeyPressed = music.requiredString("blackKeyPressed"), correctNote = music.requiredString("correctNote"),
                wrongNote = music.requiredString("wrongNote"), waitingNote = music.requiredString("waitingNote"),
                particleLeft = music.requiredString("particleLeft"), particleRight = music.requiredString("particleRight")
            ),
            effects = ThemeEffects(
                pressedGlow = effects?.get("pressedGlow")?.jsonPrimitive?.floatOrNull ?: 0.75f,
                noteTrail = effects?.get("noteTrail")?.jsonPrimitive?.floatOrNull ?: 0.55f,
                particleIntensity = effects?.get("particleIntensity")?.jsonPrimitive?.floatOrNull ?: 0.8f,
                expressiveMotion = effects?.get("expressiveMotion")?.jsonPrimitive?.booleanOrNull ?: true
            )
        ).also { theme ->
            val errors = ThemeValidator.validate(theme)
            require(errors.isEmpty()) { errors.joinToString(" ") }
        }
    }

    fun encode(theme: ElyThemeDefinition): String = json.encodeToString(
        kotlinx.serialization.json.JsonElement.serializer(),
        buildJsonObject {
            put("\$schema", "https://elytesia.app/schemas/theme-v1.schema.json")
            put("schemaVersion", theme.schemaVersion); put("id", theme.id); put("name", theme.name)
            put("author", theme.author); put("description", theme.description); put("version", theme.version)
            put("license", theme.license); put("mode", theme.mode.name.lowercase())
            put("material", theme.material.toJson()); put("music", theme.music.toJson())
            put("effects", buildJsonObject {
                put("pressedGlow", theme.effects.pressedGlow); put("noteTrail", theme.effects.noteTrail)
                put("particleIntensity", theme.effects.particleIntensity); put("expressiveMotion", theme.effects.expressiveMotion)
            })
        }
    )

    private fun MaterialThemeTokens.toJson() = buildJsonObject {
        put("primary", primary); put("onPrimary", onPrimary); put("primaryContainer", primaryContainer)
        put("onPrimaryContainer", onPrimaryContainer); put("secondary", secondary); put("onSecondary", onSecondary)
        put("secondaryContainer", secondaryContainer); put("onSecondaryContainer", onSecondaryContainer)
        put("tertiary", tertiary); put("onTertiary", onTertiary); put("background", background)
        put("onBackground", onBackground); put("surface", surface); put("onSurface", onSurface)
        put("surfaceVariant", surfaceVariant); put("onSurfaceVariant", onSurfaceVariant)
        put("error", error); put("onError", onError); put("outline", outline)
    }

    private fun MusicThemeTokens.toJson() = buildJsonObject {
        put("leftHand", leftHand); put("rightHand", rightHand); put("neutralTrack", neutralTrack)
        put("whiteKey", whiteKey); put("whiteKeyPressed", whiteKeyPressed); put("blackKey", blackKey)
        put("blackKeyPressed", blackKeyPressed); put("correctNote", correctNote); put("wrongNote", wrongNote)
        put("waitingNote", waitingNote); put("particleLeft", particleLeft); put("particleRight", particleRight)
    }

    private fun JsonObject.requiredObject(name: String) = get(name)?.jsonObject ?: error("Falta el objeto '$name'.")
    private fun JsonObject.requiredString(name: String) = get(name)?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
        ?: error("Falta el campo '$name'.")
    private fun JsonObject.optionalString(name: String) = get(name)?.jsonPrimitive?.content.orEmpty()
    private fun JsonObject.requiredInt(name: String) = get(name)?.jsonPrimitive?.intOrNull ?: error("'$name' debe ser entero.")
}
