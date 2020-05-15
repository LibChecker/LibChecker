package com.absinthe.libchecker.api.bean

const val CONFIGURATION_VERSION = 2

data class Configuration(
    val enableLibDetail: Boolean,
    val showLibName: Boolean,
    val showLibDescription: Boolean,
    val showContributor: Boolean,
    val showRelativeUrl: Boolean
)