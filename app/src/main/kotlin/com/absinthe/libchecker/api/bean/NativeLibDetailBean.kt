package com.absinthe.libchecker.api.bean

data class NativeLibDetailBean(
    val label: String,
    val contributors: List<String>,
    val iconUrl: String,
    val description: String,
    val relativeUrl: String
)