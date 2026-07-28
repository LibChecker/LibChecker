package com.absinthe.libchecker.domain.app.detail.navigation

import com.absinthe.libchecker.annotation.LibType

data class DetailReferenceNavigation(
  @LibType val type: Int,
  val tabPosition: Int,
  val targetName: String
)
