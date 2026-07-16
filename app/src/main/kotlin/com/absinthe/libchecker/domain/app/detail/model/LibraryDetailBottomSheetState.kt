package com.absinthe.libchecker.domain.app.detail.model

import androidx.annotation.DrawableRes

data class LibraryDetailHeaderDisplay(
  @DrawableRes val iconRes: Int,
  val isSimpleColorIcon: Boolean
)

data class LibraryDetailContentDisplay(
  val locales: List<LibraryDetailLocaleDisplay>,
  val selectedLocaleTag: String
) {
  init {
    require(locales.isNotEmpty()) { "Library detail content requires at least one locale." }
    require(locales.any { it.localeTag == selectedLocaleTag }) {
      "Selected locale must be present in library detail content."
    }
  }
}

data class LibraryDetailLocaleDisplay(
  val localeTag: String,
  val localeName: String,
  val items: List<DetailInfoItemDisplay>
)

sealed interface LibraryDetailBottomSheetState {
  val title: String

  data class Loading(
    override val title: String,
    val header: LibraryDetailHeaderDisplay? = null
  ) : LibraryDetailBottomSheetState

  data class Content(
    override val title: String,
    val header: LibraryDetailHeaderDisplay,
    val content: LibraryDetailContentDisplay
  ) : LibraryDetailBottomSheetState

  data class NotFound(
    override val title: String,
    val header: LibraryDetailHeaderDisplay
  ) : LibraryDetailBottomSheetState
}
