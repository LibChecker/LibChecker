package com.absinthe.libchecker.bean

data class TrackListItem(
    val label: String,
    val packageName: String,
    var switchState: Boolean = false
)
