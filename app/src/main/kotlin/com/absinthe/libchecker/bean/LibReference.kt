package com.absinthe.libchecker.bean

import com.absinthe.libchecker.ui.main.LibReferenceActivity

data class LibReference(
    val libName: String,
    val referredCount: Int,
    val type :LibReferenceActivity.Type
)