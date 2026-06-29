package com.absinthe.libchecker.domain.settings.model

import androidx.annotation.DrawableRes

data class GetUpdatesItem(
  val text: String,
  @DrawableRes val iconRes: Int,
  val action: GetUpdatesAction
)

sealed interface GetUpdatesAction {
  data class OpenUri(val uri: String) : GetUpdatesAction
  data object OpenInAppUpdate : GetUpdatesAction
}
