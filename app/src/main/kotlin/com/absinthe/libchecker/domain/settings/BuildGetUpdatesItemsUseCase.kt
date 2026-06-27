package com.absinthe.libchecker.domain.settings

import android.content.Context
import androidx.annotation.DrawableRes
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.URLManager

class BuildGetUpdatesItemsUseCase(
  private val context: Context
) {

  operator fun invoke(): List<GetUpdatesItem> {
    val items = listOf(
      GetUpdatesItem(
        text = "GitHub",
        iconRes = R.drawable.ic_github,
        action = GetUpdatesAction.OpenUri(URLManager.GITHUB_REPO_PAGE)
      ),
      GetUpdatesItem(
        text = "Google Play",
        iconRes = com.absinthe.lc.rulesbundle.R.drawable.ic_lib_google,
        action = GetUpdatesAction.OpenUri(URLManager.PLAY_STORE_DETAIL_PAGE)
      ),
      GetUpdatesItem(
        text = "Telegram",
        iconRes = com.absinthe.lc.rulesbundle.R.drawable.ic_lib_telegram,
        action = GetUpdatesAction.OpenUri(URLManager.TELEGRAM_RELEASES)
      ),
      GetUpdatesItem(
        text = "F-Droid",
        iconRes = R.drawable.ic_fdroid,
        action = GetUpdatesAction.OpenUri(URLManager.FDROID_PAGE)
      )
    )

    return if (context.resources.getBoolean(R.bool.is_foss)) {
      items + GetUpdatesItem(
        text = context.getString(R.string.settings_get_updates_in_app),
        iconRes = R.drawable.ic_logo,
        action = GetUpdatesAction.OpenInAppUpdate
      )
    } else {
      items
    }
  }
}

data class GetUpdatesItem(
  val text: String,
  @DrawableRes val iconRes: Int,
  val action: GetUpdatesAction
)

sealed interface GetUpdatesAction {
  data class OpenUri(val uri: String) : GetUpdatesAction
  data object OpenInAppUpdate : GetUpdatesAction
}
