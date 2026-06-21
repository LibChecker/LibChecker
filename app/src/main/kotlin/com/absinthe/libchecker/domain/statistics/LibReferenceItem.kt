package com.absinthe.libchecker.domain.statistics

import com.absinthe.libchecker.annotation.LibType
import com.absinthe.rulesbundle.Rule

data class LibReferenceItem(
  val libName: String,
  val rule: Rule?,
  val referredList: Set<String>,
  @LibType val type: Int
)
