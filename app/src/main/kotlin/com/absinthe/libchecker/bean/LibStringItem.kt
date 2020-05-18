package com.absinthe.libchecker.bean

import com.absinthe.libchecker.constant.LibChip

data class LibStringItem(val name: String, val size: Long, val libInfo: LibChip = LibChip(0, "0"))