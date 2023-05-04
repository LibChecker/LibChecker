package com.microsoft.appcenter.analytics

import com.microsoft.appcenter.AppCenterService

object Analytics : AppCenterService {

    fun trackEvent(name: String, properties: Map<String, String>) {

    }

    fun trackEvent(name: String, properties: Map<String, String>, flags: Int) {

    }

    fun trackEvent(name: String, properties: EventProperties) {

    }

    fun trackEvent(name: String, properties: EventProperties, flags: Int) {

    }

}
