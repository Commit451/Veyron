package com.commit451.driveappfolderviewer.sample

import com.squareup.moshi.Json
import java.util.*

class Dog {
    @field:Json(name = "name")
    var name: String = ""
    @field:Json(name = "created")
    var created: Date? = null
}