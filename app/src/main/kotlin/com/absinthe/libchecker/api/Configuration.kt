package com.absinthe.libchecker.api

const val CONFIGURATION_VERSION = 1

data class Configuration(
    val enableLibDetail: Boolean,
    val showIcon: Boolean,
    val showLibName: Boolean,
    val showLibDescription: Boolean,
    val showContributor: Boolean,
    val showRelativeUrl: Boolean
)