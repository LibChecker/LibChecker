package com.absinthe.libchecker.domain.app.detail.action

import android.content.Context
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.XposedInfoAction
import com.absinthe.libchecker.domain.app.detail.model.XposedInfoBottomSheetDisplay
import com.absinthe.libchecker.domain.app.detail.model.XposedInfoItemDisplay
import com.absinthe.libchecker.domain.app.detail.model.XposedInfoTextStyle

fun buildXposedInfoBottomSheetDisplay(
  context: Context,
  info: XposedModuleInfo
): XposedInfoBottomSheetDisplay {
  return buildXposedInfoBottomSheetDisplay(info, context.displayStrings())
}

private fun Context.displayStrings(): XposedInfoDisplayStrings {
  return XposedInfoDisplayStrings(
    minVersion = getString(R.string.lib_detail_xposed_min_version),
    targetVersion = getString(R.string.lib_detail_xposed_target_version),
    staticScope = getString(R.string.lib_detail_xposed_static_scope),
    defaultScope = getString(R.string.lib_detail_xposed_default_scope),
    initClass = getString(R.string.lib_detail_xposed_init_class),
    description = getString(R.string.lib_detail_description_tip),
    trueValue = "True"
  )
}

internal data class XposedInfoDisplayStrings(
  val minVersion: String,
  val targetVersion: String,
  val staticScope: String,
  val defaultScope: String,
  val initClass: String,
  val description: String,
  val trueValue: String
)

internal fun buildXposedInfoBottomSheetDisplay(
  info: XposedModuleInfo,
  strings: XposedInfoDisplayStrings
): XposedInfoBottomSheetDisplay {
  return XposedInfoBottomSheetDisplay(
    appName = info.appName,
    settingsAction = XposedInfoAction.OpenSettings(info.settingsIntent),
    items = buildList {
      addTextItem(
        value = info.minVersion,
        iconRes = R.drawable.ic_app_prop,
        tip = strings.minVersion,
        textStyle = XposedInfoTextStyle.Title
      )
      addTextItem(
        value = info.targetVersion,
        iconRes = R.drawable.ic_app_prop,
        tip = strings.targetVersion,
        textStyle = XposedInfoTextStyle.Title
      )
      if (info.staticScope) {
        add(
          XposedInfoItemDisplay(
            iconRes = R.drawable.ic_app_prop,
            tip = strings.staticScope,
            text = strings.trueValue,
            textStyle = XposedInfoTextStyle.Title
          )
        )
      }
      addTextItem(
        value = info.defaultScope,
        iconRes = R.drawable.ic_app_prop,
        tip = strings.defaultScope,
        textStyle = XposedInfoTextStyle.Title
      )
      addTextItem(
        value = info.javaInitClasses,
        iconRes = R.drawable.ic_app_prop,
        tip = "${strings.initClass} (Java)",
        textStyle = XposedInfoTextStyle.Title
      )
      addTextItem(
        value = info.nativeInitLibraries,
        iconRes = R.drawable.ic_app_prop,
        tip = "${strings.initClass} (Native)",
        textStyle = XposedInfoTextStyle.Title
      )
      addTextItem(
        value = info.legacyInitClass,
        iconRes = R.drawable.ic_app_prop,
        tip = "${strings.initClass} (Legacy)",
        textStyle = XposedInfoTextStyle.Title
      )
      addTextItem(
        value = info.description,
        iconRes = R.drawable.ic_content,
        tip = strings.description,
        textStyle = XposedInfoTextStyle.Body
      )
    }
  )
}

private fun MutableList<XposedInfoItemDisplay>.addTextItem(
  value: String?,
  @androidx.annotation.DrawableRes iconRes: Int,
  tip: String,
  textStyle: XposedInfoTextStyle
) {
  value?.takeIf(String::isNotBlank)?.let {
    add(
      XposedInfoItemDisplay(
        iconRes = iconRes,
        tip = tip,
        text = it,
        textStyle = textStyle
      )
    )
  }
}
