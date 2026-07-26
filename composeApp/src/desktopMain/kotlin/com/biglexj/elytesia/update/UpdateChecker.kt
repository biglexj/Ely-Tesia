package com.biglexj.elytesia.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Implementación Desktop JVM del verificador de actualizaciones.
 * Consulta la GitHub API de releases públicas para obtener la última versión disponible.
 */
actual object UpdateChecker {
    actual val currentVersion: String = "1.0.6"

    private const val GITHUB_RELEASES_API =
        "https://api.github.com/repos/biglexj/Ely-Tesia/releases/latest"
    private const val GITHUB_RELEASES_URL =
        "https://github.com/biglexj/Ely-Tesia/releases/latest"

    actual suspend fun checkForUpdates(): UpdateResult = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(GITHUB_RELEASES_API)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "Ely-Tesia/$currentVersion")
            connection.connectTimeout = 8000
            connection.readTimeout = 10000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val body = connection.inputStream.bufferedReader().readText()
                val tagVersion = extractJsonString(body, "tag_name")
                    ?.removePrefix("v") ?: "desconocido"
                val htmlUrl = extractJsonString(body, "html_url") ?: GITHUB_RELEASES_URL
                val body2 = extractJsonString(body, "body") ?: ""

                val isNewer = isVersionNewer(tagVersion, currentVersion)
                UpdateResult(
                    latestVersion = tagVersion,
                    releaseUrl = htmlUrl,
                    releaseNotes = sanitizeReleaseNotes(body2),
                    isUpdateAvailable = isNewer
                )
            } else {
                UpdateResult(
                    latestVersion = "?",
                    releaseUrl = GITHUB_RELEASES_URL,
                    releaseNotes = "",
                    isUpdateAvailable = false,
                    error = "Error HTTP ${connection.responseCode}"
                )
            }
        }.getOrElse { e ->
            UpdateResult(
                latestVersion = "?",
                releaseUrl = GITHUB_RELEASES_URL,
                releaseNotes = "",
                isUpdateAvailable = false,
                error = e.message ?: "Error de red"
            )
        }
    }

    /** Extrae un valor string de un campo JSON de forma sencilla sin dependencia de librerías. */
    private fun extractJsonString(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"(.*?)\"".toRegex(RegexOption.DOT_MATCHES_ALL)
        return pattern.find(json)?.groupValues?.getOrNull(1)
            ?.replace("\\n", "\n")
            ?.replace("\\\"", "\"")
            ?.replace("\\\\", "\\")
    }

    /**
     * Compara dos versiones en formato semver simplificado (mayor.menor.parche).
     * @return true si [remote] es más nueva que [local].
     */
    private fun isVersionNewer(remote: String, local: String): Boolean {
        fun parts(v: String) = v.split(".").mapNotNull { it.filter(Char::isDigit).toIntOrNull() }
        val r = parts(remote)
        val l = parts(local)
        for (i in 0 until maxOf(r.size, l.size)) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv > lv) return true
            if (rv < lv) return false
        }
        return false
    }
}
