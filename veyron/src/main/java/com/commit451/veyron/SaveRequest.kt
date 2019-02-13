package com.commit451.veyron

import com.google.api.client.http.ByteArrayContent


/**
 * Create a request to save data to Google Drive
 */
class SaveRequest(val content: ByteArrayContent, val title: String)