package com.absinthe.libchecker.features.statistics.bean

import android.os.Parcelable
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.rulesbundle.Rule
import com.chad.library.adapter.base.entity.node.BaseNode
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class LibReference(
  val libName: String,
  val rule: Rule?,
  val referredList: Set<String>,
  @LibType val type: Int,
  override val childNode: @RawValue MutableList<BaseNode>? = null
) : BaseNode(),
  Parcelable
