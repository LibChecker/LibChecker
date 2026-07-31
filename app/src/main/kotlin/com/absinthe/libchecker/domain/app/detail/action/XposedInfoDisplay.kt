package com.absinthe.libchecker.domain.app.detail.action

import android.content.Context
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.XposedInfoAction
import com.absinthe.libchecker.domain.app.detail.model.XposedInfoBottomSheetDisplay
import com.absinthe.libchecker.domain.app.detail.model.XposedInfoItemDisplay
import com.absinthe.libchecker.domain.app.detail.model.XposedInfoTextStyle
import com.absinthe.libchecker.domain.app.detail.model.XposedScopeAppDisplay

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
    autoHotReload = getString(R.string.lib_detail_xposed_auto_hot_reload),
    hotReloadDeclared = getString(R.string.lib_detail_xposed_hot_reload_declared),
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
  val autoHotReload: String,
  val hotReloadDeclared: String,
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
      if (info.autoHotReloadDeclared) {
        addTextItem(
          value = strings.hotReloadDeclared,
          iconRes = R.drawable.ic_refresh,
          tip = strings.autoHotReload,
          textStyle = XposedInfoTextStyle.Title
        )
      }
      if (info.staticScope) {
        add(
          XposedInfoItemDisplay.Text(
            iconRes = R.drawable.ic_app_prop,
            tip = strings.staticScope,
            text = strings.trueValue,
            textStyle = XposedInfoTextStyle.Title
          )
        )
      }
      info.defaultScope?.takeIf(List<XposedScopeAppInfo>::isNotEmpty)?.let { scopeApps ->
        val literalScope = scopeApps.singleOrNull()
          ?.packageName
          ?.takeIf { it == "*" || it == "system" }
        if (literalScope != null) {
          add(
            XposedInfoItemDisplay.Text(
              iconRes = R.drawable.ic_app_prop,
              tip = strings.defaultScope,
              text = literalScope,
              textStyle = XposedInfoTextStyle.Title
            )
          )
        } else {
          add(
            XposedInfoItemDisplay.ScopeApps(
              iconRes = R.drawable.ic_app_prop,
              tip = strings.defaultScope,
              apps = scopeApps
                .sortedBy { it.packageInfo == null }
                .map {
                  XposedScopeAppDisplay(
                    packageName = it.packageName,
                    label = it.label,
                    packageInfo = it.packageInfo
                  )
                }
            )
          )
        }
      }
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
      XposedInfoItemDisplay.Text(
        iconRes = iconRes,
        tip = tip,
        text = it,
        textStyle = textStyle
      )
    )
  }
}
