package com.commit451.veyron

import com.google.api.services.drive.model.File

/**
 * Cache of folders
 */
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
internal class FoldersCache : Cache<File> {

    private val cache = mutableMapOf<String, File>()

    override fun get(path: String): File? {
        return cache[path]
    }

    override fun put(path: String, file: File) {
        cache[path] = file
    }

    override fun remove(path: String) {
        cache.remove(path)
    }

    override fun clear() {
        cache.clear()
    }
}