package com.biglexj.elytesia.storage

import java.io.File

class DesktopLocalStorage : LocalStorage {
    private val stateFile: File = File(
        System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"),
        "ElyTesia/state.txt"
    )

    override fun read(): String? = runCatching {
        stateFile.takeIf { it.isFile }?.readText()
    }.getOrNull()

    override fun write(contents: String) {
        runCatching {
            stateFile.parentFile?.mkdirs()
            val temporary = File(stateFile.parentFile, "state.tmp")
            temporary.writeText(contents)
            if (stateFile.exists()) stateFile.delete()
            temporary.renameTo(stateFile)
        }.onFailure { println("No se pudo guardar el estado local: ${it.message}") }
    }
}
