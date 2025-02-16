package com.absinthe.libchecker.features.applist.detail.ui.adapter.node

class AbiLabelNode(val abi: Int, val active: Boolean, val action: (() -> Unit)? = null) : BaseAbiLabelsNode()
