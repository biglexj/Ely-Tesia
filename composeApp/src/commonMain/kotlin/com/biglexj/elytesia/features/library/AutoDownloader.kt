package com.biglexj.elytesia.features.library

import com.biglexj.elytesia.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Gestor de Autodescarga de Recursos, Canciones Demo y Actualizaciones para Windows Desktop JVM y Android.
 */
object AutoDownloader {

    /**
     * Descarga un archivo desde una URL pública y lo guarda en el directorio local de la plataforma.
     */
    suspend fun downloadFile(urlStr: String, destinationFile: File): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            destinationFile.parentFile?.mkdirs()
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 15000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.use { input ->
                    destinationFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                destinationFile
            } else {
                error("Error HTTP ${connection.responseCode} al descargar recurso.")
            }
        }
    }

    /**
     * Comprueba y descarga canciones demo o paquetes MIDI actualizados para el catálogo.
     */
    suspend fun syncOnlineDemos(
        targetDir: File,
        onProgress: (String) -> Unit
    ): List<File> = withContext(Dispatchers.IO) {
        val downloadedFiles = mutableListOf<File>()
        val demoSources = listOf(
            "https://raw.githubusercontent.com/biglexj/Ely-Tesia/main/assets/midi/demo_song_1.mid" to "demo_song_1.mid",
            "https://raw.githubusercontent.com/biglexj/Ely-Tesia/main/assets/midi/demo_song_2.mid" to "demo_song_2.mid"
        )

        for ((url, filename) in demoSources) {
            val file = File(targetDir, filename)
            if (!file.exists()) {
                onProgress("Descargando $filename...")
                downloadFile(url, file).onSuccess {
                    downloadedFiles.add(it)
                }
            } else {
                downloadedFiles.add(file)
            }
        }
        downloadedFiles
    }
}
