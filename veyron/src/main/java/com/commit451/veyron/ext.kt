package com.commit451.veyron

import com.google.api.services.drive.model.File

fun File.identify(): String {
    return "${this.name}:${this.id}"
}

fun File.isFolder(): Boolean {
    return mimeType == MIME_TYPE_FOLDER
}