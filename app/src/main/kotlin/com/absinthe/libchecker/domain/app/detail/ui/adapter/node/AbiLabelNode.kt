package com.absinthe.libchecker.domain.app.detail.ui.adapter.node

import com.chad.library.adapter.base.entity.node.BaseNode

class AbiLabelNode(
  val abi: Int,
  val active: Boolean,
  val contentDescription: String,
  val is64Bit: Boolean,
  val action: (() -> Unit)? = null
) : BaseNode() {
  override val childNode: MutableList<BaseNode>? = null
}
