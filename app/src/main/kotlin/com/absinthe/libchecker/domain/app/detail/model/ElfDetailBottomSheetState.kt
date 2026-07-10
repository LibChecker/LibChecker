package com.absinthe.libchecker.domain.app.detail.model

import androidx.annotation.DrawableRes

sealed interface ElfDetailBottomSheetState {
  val title: String

  @get:DrawableRes
  val iconRes: Int

  data class Loading(
    override val title: String,
    @DrawableRes override val iconRes: Int
  ) : ElfDetailBottomSheetState

  data class Content(
    override val title: String,
    @DrawableRes override val iconRes: Int,
    val dependenciesText: String,
    val entryPointsText: String,
    val isStripped: Boolean
  ) : ElfDetailBottomSheetState
}
