package com.absinthe.libchecker.bean

import com.absinthe.libchecker.constant.LibChip
import com.absinthe.libchecker.ui.main.LibReferenceActivity

data class LibReference(
    val libName: String,
    val chip: LibChip?,
    val referredCount: Int,
    val type :LibReferenceActivity.Type
)