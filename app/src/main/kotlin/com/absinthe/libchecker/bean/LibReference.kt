package com.absinthe.libchecker.bean

import android.os.Parcelable
import com.absinthe.libchecker.annotation.LibType
import com.chad.library.adapter.base.entity.node.BaseNode
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class LibReference(
  val libName: String,
  val chip: LibChip?,
  val referredList: Set<String>,
  @LibType val type: Int,
  override val childNode: @RawValue MutableList<BaseNode>? = null,
) : Parcelable, BaseNode()
