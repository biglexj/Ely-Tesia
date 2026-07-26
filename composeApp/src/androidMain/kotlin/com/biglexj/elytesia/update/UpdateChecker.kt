package com.biglexj.elytesia.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Implementación Android del verificador de actualizaciones.
 * Idéntica a Desktop JVM en comportamiento — utiliza la misma GitHub Releases API.
 * La paridad de funcionalidades entre plataformas es un requisito crítico del proyecto.
 */
actual object UpdateChecker {
    actual val currentVersion: String = "1.0.8"

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
            connection.setRequestProperty("User-Agent", "Ely-Tesia-Android/$currentVersion")
            connection.connectTimeout = 8000
            connection.readTimeout = 10000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val body = connection.inputStream.bufferedReader().readText()
                val tagVersion = extractJsonString(body, "tag_name")
                    ?.removePrefix("v") ?: "desconocido"
                val htmlUrl = extractJsonString(body, "html_url") ?: GITHUB_RELEASES_URL
                val notes = extractJsonString(body, "body") ?: ""

                UpdateResult(
                    latestVersion = tagVersion,
                    releaseUrl = htmlUrl,
                    releaseNotes = sanitizeReleaseNotes(notes),
                    isUpdateAvailable = isVersionNewer(tagVersion, currentVersion)
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

    private fun extractJsonString(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"(.*?)\"".toRegex(RegexOption.DOT_MATCHES_ALL)
        return pattern.find(json)?.groupValues?.getOrNull(1)
            ?.replace("\\n", "\n")
            ?.replace("\\\"", "\"")
            ?.replace("\\\\", "\\")
    }

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
