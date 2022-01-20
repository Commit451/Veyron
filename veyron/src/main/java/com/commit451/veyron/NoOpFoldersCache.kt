package com.commit451.veyron

import com.google.api.services.drive.model.File

/**
 * Does nothing
 */
@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
internal class NoOpFoldersCache : Cache<File> {

    override fun get(path: String): File? {
        return null
    }

    override fun put(path: String, file: File) {
    }

    override fun remove(path: String) {
    }

    override fun clear() {
    }
}