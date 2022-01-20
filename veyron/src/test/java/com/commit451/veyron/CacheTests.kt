package com.commit451.veyron

import com.google.api.services.drive.model.File
import org.junit.Assert.assertNotNull
import org.junit.Test

class CacheTests {

    private val cache = FoldersCache()

    @Test
    fun `cache hit`() {
        val path = "journals/p"
        val file = File()
            .setMimeType(MIME_TYPE_FOLDER)
            .setName("p")
        cache.put(path, file)

        val cachedFile = cache.get(path)
        assertNotNull(cachedFile)
    }
}