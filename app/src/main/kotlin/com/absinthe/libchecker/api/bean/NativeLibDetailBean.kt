package com.absinthe.libchecker.api.bean

data class NativeLibDetailBean(
    val label: String,
    val team: String,
    val iconUrl: String,
    val contributors: List<String>,
    val description: String,
    val relativeUrl: String
)