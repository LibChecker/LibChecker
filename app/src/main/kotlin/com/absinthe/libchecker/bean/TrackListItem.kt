package com.absinthe.libchecker.bean

import android.graphics.drawable.Drawable

data class TrackListItem(
    val icon: Drawable,
    val label: String,
    val packageName: String,
    var switchState: Boolean = false
)