package com.absinthe.libchecker.domain.app.detail.model

import android.content.Intent
import android.content.pm.PackageInfo
import androidx.annotation.DrawableRes

data class XposedInfoBottomSheetDisplay(
  val appName: String,
  val settingsAction: XposedInfoAction,
  val items: List<XposedInfoItemDisplay>
)

sealed interface XposedInfoItemDisplay {

  @get:DrawableRes
  val iconRes: Int

  val tip: String

  data class Text(
    @DrawableRes override val iconRes: Int,
    override val tip: String,
    val text: String,
    val textStyle: XposedInfoTextStyle
  ) : XposedInfoItemDisplay

  data class ScopeApps(
    @DrawableRes override val iconRes: Int,
    override val tip: String,
    val apps: List<XposedScopeAppDisplay>
  ) : XposedInfoItemDisplay
}

data class XposedScopeAppDisplay(
  val packageName: String,
  val label: String,
  val packageInfo: PackageInfo?
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
