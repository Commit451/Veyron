package com.commit451.veyron

/**
 * Create a request to save data to Google Drive
 */
sealed class SaveRequest {
    /**
     * Create a request to save data to Google Drive with [ByteArrayContent]
     */
    class ByteArrayContent(val title: kotlin.String, val content: com.google.api.client.http.ByteArrayContent) : SaveRequest()

    /**
     * Create a request to save data to Google Drive with [String]
     */
    class String(val title: kotlin.String, val content: kotlin.String) : SaveRequest()

    /**
     * Create a request to save data to Google Drive with [T]
     */
    class Document<T>(val title: kotlin.String, val type: Class<T>, val item: T) : SaveRequest()
}