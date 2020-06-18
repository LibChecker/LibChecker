package com.absinthe.libchecker.bean

import com.chad.library.adapter.base.entity.MultiItemEntity

const val TYPE_NATIVE = 0
const val TYPE_COMPONENT = 1

const val ADDED = 0
const val REMOVED = 1
const val CHANGED = 2

data class SnapshotDetailItem(
    val title: String,
    val extra: String,
    val diffType: Int,
    override val itemType: Int
) : MultiItemEntity