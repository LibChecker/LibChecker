package com.absinthe.libchecker.domain.app.detail.abi

data class AppDetailAbiLabelData(
  val is64Bit: Boolean,
  val labels: List<AppDetailAbiLabel>
)

data class AppDetailAbiLabel(
  val abi: Int,
  val isActive: Boolean,
  val contentDescription: String,
  val is64Bit: Boolean,
  val opensMultiArchInfo: Boolean = false
)
