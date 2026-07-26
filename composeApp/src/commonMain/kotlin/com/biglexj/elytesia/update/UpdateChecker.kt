package com.biglexj.elytesia.update

/**
 * Checker de Actualizaciones Multiplataforma (Windows Desktop JVM & Android).
 * Consulta el endpoint de GitHub Releases para comparar la versión instalada con la más reciente.
 */
expect object UpdateChecker {
    val currentVersion: String

    /**
     * Consulta la última release pública del repositorio y devuelve la información.
     * @return [UpdateResult] con la información de la última versión disponible.
     */
    suspend fun checkForUpdates(): UpdateResult
}

data class UpdateResult(
    val latestVersion: String,
    val releaseUrl: String,
    val releaseNotes: String,
    val isUpdateAvailable: Boolean,
    val error: String? = null
)

/**
 * Sanitiza las notas de lanzamiento procedentes de la API de GitHub Releases.
 * Elimina Markdown crudo, encabezados, negritas, enlaces y caracteres especiales para presentarlos limpios en la UI.
 */
fun sanitizeReleaseNotes(notes: String): String {
    if (notes.isBlank()) return "Sin notas de lanzamiento."
    val cleaned = notes
        .replace("\r\n", "\n")
        .replace(Regex("<[^>]*>"), "") // Eliminar etiquetas HTML
        .replace(Regex("##*\\s*"), "") // Eliminar marcadores de título markdown (#, ##, ###)
        .replace(Regex("\\*\\*|__|\\*|_|`"), "") // Eliminar negrita, cursiva y código en línea
        .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1") // Reemplazar enlaces por su texto descriptivo
        .lines()
        .map { it.trim().removePrefix("-").removePrefix("*").trim() }
        .filter { it.isNotBlank() && !it.startsWith("http") }

    if (cleaned.isEmpty()) return "Actualización de mantenimiento y estabilidad."
    
    return cleaned.take(3).joinToString("\n• ", prefix = "• ")
}
