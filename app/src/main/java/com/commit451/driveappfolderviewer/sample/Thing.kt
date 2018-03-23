package com.commit451.driveappfolderviewer.sample

import com.squareup.moshi.Json

/**
 * A thing
 */
class Thing {

    @field:Json(name = "stuff")
    var stuff: String = ""
}