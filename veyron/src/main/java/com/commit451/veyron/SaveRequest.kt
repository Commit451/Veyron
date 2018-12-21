package com.commit451.veyron


/**
 * Create a request to save data to Google Drive
 */
class SaveRequest<T>(val type: Class<T>, val item: T, val title: String)