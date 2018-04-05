package com.commit451.veyron

import com.google.android.gms.drive.MetadataBuffer


internal fun MetadataBuffer.isEmpty(): Boolean {
    return count == 0
}