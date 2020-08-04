package com.absinthe.libchecker.bean

import com.absinthe.libchecker.constant.LibChip
import com.absinthe.libchecker.constant.LibType

data class LibReference(
    val libName: String,
    val chip: LibChip?,
    val referredCount: Int,
    @LibType val type :Int
)