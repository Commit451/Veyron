package com.commit451.veyron

import com.google.api.services.drive.model.File

/**
 * Create a request to save data to Google Drive
 */
sealed class SaveRequest {
    /**
     * Create a request to save data to Google Drive with [ByteArrayContent]
     */
    data class ByteArrayContent(
        val title: kotlin.String,
        val mediaContent: com.google.api.client.http.ByteArrayContent,
        val content: File? = null,
    ) : SaveRequest()

    /**
     * Create a request to save data to Google Drive with [String]
     */
    data class String(
        val title: kotlin.String,
        val mediaContent: kotlin.String,
        val content: File? = null,
    ) : SaveRequest()

    /**
     * Create a request to save data to Google Drive with [T]
     */
    data class Document<T>(
        val title: kotlin.String,
        val type: Class<T>,
        val item: T,
        val content: File? = null,
    ) : SaveRequest()

    /**
     * Create a request to save data to Google Drive with just metadata file content
     */
    data class Metadata(
        val title: kotlin.String,
        val content: File? = null,
    ) : SaveRequest()
}