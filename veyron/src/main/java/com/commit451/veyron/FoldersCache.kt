package com.commit451.veyron

import com.google.api.services.drive.model.File

/**
 * Cache of folders
 */
internal class FoldersCache {

    private val cache = mutableMapOf<String, File>()

    fun get(path: String): File? {
        return cache[path]
    }

    fun put(path: String, file: File) {
        cache[path] = file
    }
}