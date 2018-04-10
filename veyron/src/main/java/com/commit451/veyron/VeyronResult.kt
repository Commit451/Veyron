package com.commit451.veyron

/**
 * Contains the result, as an optional value. Result is nullable, so always expect and account for this
 */
class VeyronResult<out T>(val result: T?)