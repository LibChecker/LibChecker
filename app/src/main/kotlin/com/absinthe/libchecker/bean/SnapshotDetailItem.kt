package com.absinthe.libchecker.bean

import com.absinthe.libchecker.ui.main.LibReferenceActivity


const val ADDED = 0
const val REMOVED = 1
const val CHANGED = 2

data class SnapshotDetailItem(
    val title: String,
    val extra: String,
    val diffType: Int,
    val itemType: LibReferenceActivity.Type
)