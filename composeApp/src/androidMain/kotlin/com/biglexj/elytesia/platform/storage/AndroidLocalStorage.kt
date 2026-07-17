package com.biglexj.elytesia.storage

import android.content.Context

class AndroidLocalStorage(context: Context) : LocalStorage {
    private val stateFile = context.filesDir.resolve("elytesia-state.txt")

    override fun read(): String? = runCatching {
        stateFile.takeIf { it.isFile }?.readText()
    }.getOrNull()

    override fun write(contents: String) {
        runCatching { stateFile.writeText(contents) }
    }
}
