package com.commit451.veyron

internal interface Cache<T> {

    fun get(path: String): T?

    fun put(path: String, t: T)

    fun remove(path: String)

    fun clear()
}