package com.absinthe.libchecker.domain.app.detail.model

import com.absinthe.rulesbundle.Rule

data class LibStringItemChip(
  val item: LibStringItem,
  val rule: Rule?,
  val labels: List<String> = emptyList(),
  val nativeDisplayData: NativeLibraryItemDisplayData? = null
)

data class NativeLibraryItemDisplayData(
  val sizeText: String,
  val labels: List<String>
)
