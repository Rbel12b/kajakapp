package com.rbel12b.kajakapp.data.cache

import java.io.File

class FileCache(private val dir: File) {

    fun read(key: String): String? = try {
        val f = File(dir, sanitize(key) + ".json")
        if (f.exists()) f.readText() else null
    } catch (_: Exception) { null }

    fun write(key: String, json: String) {
        try {
            dir.mkdirs()
            File(dir, sanitize(key) + ".json").writeText(json)
        } catch (_: Exception) {}
    }

    fun delete(key: String) {
        try { File(dir, sanitize(key) + ".json").delete() } catch (_: Exception) {}
    }

    private fun sanitize(key: String) = key.replace(Regex("[^a-zA-Z0-9_-]"), "_")
}
