package com.absinthe.libchecker.api.bean

const val CONFIGURATION_VERSION = 3

data class Configuration(
    val enableLibDetail: Boolean,
    val showLibName: Boolean,
    val showTeamName: Boolean,
    val showLibDescription: Boolean,
    val showContributor: Boolean,
    val showRelativeUrl: Boolean
)