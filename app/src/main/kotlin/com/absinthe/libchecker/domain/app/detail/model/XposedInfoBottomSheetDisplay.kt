package com.absinthe.libchecker.domain.app.detail.model

import android.content.Intent
import androidx.annotation.DrawableRes

data class XposedInfoBottomSheetDisplay(
  val appName: String,
  val settingsAction: XposedInfoAction,
  val items: List<XposedInfoItemDisplay>
)

data class XposedInfoItemDisplay(
  @DrawableRes val iconRes: Int,
  val tip: String,
  val text: String,
  val textStyle: XposedInfoTextStyle
)

enum class XposedInfoTextStyle {
  Title,
  Body
}

sealed interface XposedInfoAction {
  data class OpenSettings(
    val intent: Intent?
  ) : XposedInfoAction
}
