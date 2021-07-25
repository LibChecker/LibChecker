package com.absinthe.libchecker.api.bean

import androidx.annotation.Keep

@Keep
data class LibDetailBean(
    val label: String,
    val team: String,
    val iconUrl: String,
    val contributors: List<String>,
    val description: String,
    val relativeUrl: String
)
