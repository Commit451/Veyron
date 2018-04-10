package com.commit451.veyron

import com.google.android.gms.drive.MetadataChangeSet

/**
 * Create a request to save data to Google Drive
 */
class SaveRequest<T>(val type: Class<T>, val item: T, val metadataChangeSet: MetadataChangeSet)