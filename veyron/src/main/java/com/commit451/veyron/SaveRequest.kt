package com.commit451.veyron

import com.google.android.gms.drive.MetadataChangeSet

class SaveRequest<T>(val type: Class<T>, val item: T, val metadataChangeSet: MetadataChangeSet)