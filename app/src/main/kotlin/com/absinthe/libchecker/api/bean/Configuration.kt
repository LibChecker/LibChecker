package com.absinthe.libchecker.api.bean

import androidx.annotation.Keep

const val CONFIGURATION_VERSION = 4

@Keep
data class Configuration(
    val enableLibDetail: Boolean = false,
    val enableComponentsDetail: Boolean = false,
    val showLibName: Boolean = false,
    val showTeamName: Boolean = false,
    val showLibDescription: Boolean = false,
    val showContributor: Boolean = false,
    val showRelativeUrl: Boolean = false
)