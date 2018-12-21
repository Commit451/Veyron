package com.commit451.driveappfolderviewer.sample

import com.squareup.moshi.Json

/**
 * Hold all the dogs in one file
 */
class DogsResponse {
    @field:Json(name = "dogs")
    var dogs: MutableList<Dog>? = null
}