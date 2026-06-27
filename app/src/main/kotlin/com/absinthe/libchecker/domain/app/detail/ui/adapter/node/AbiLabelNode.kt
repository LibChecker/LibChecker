package com.absinthe.libchecker.domain.app.detail.ui.adapter.node

class AbiLabelNode(
  val abi: Int,
  val active: Boolean,
  val contentDescription: String,
  val is64Bit: Boolean,
  val action: (() -> Unit)? = null
) : BaseAbiLabelsNode()
