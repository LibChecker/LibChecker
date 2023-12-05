package com.microsoft.appcenter.analytics

import com.microsoft.appcenter.AppCenterService

object Analytics : AppCenterService {

    @JvmStatic
    fun trackEvent(name: String, properties: Map<String, String>) {

    }

    @JvmStatic
    fun trackEvent(name: String, properties: Map<String, String>, flags: Int) {

    }

    @JvmStatic
    fun trackEvent(name: String, properties: EventProperties) {

    }

    @JvmStatic
    fun trackEvent(name: String, properties: EventProperties, flags: Int) {

    }

}
