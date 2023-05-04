package com.microsoft.appcenter.analytics

import java.util.Date

class EventProperties {
    fun set(key: String, value: Boolean) = this

    fun set(key: String, value: Date) = this

    fun set(key: String, value: Double) = this

    fun set(key: String, value: Long) = this

    fun set(key: String, value: String?) = this

}
