package com.absinthe.libchecker.domain.app.list.model

import android.os.Build
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.ui.app.MenuOptionItem

data class AdvancedMenuBottomSheetState(
  val displayOptions: Int,
  val demoItem: LCItem,
  val filterOptions: List<MenuOptionItem>,
  val viewOptions: List<MenuOptionItem>
)

sealed interface AdvancedMenuAction {
  data class SortChanged(
    val displayOptions: Int
  ) : AdvancedMenuAction

  data class DisplayOptionChanged(
    val item: MenuOptionItem,
    val isChecked: Boolean
  ) : AdvancedMenuAction
}

fun buildAdvancedMenuBottomSheetState(
  displayOptions: Int,
  targetApi: Int = Build.VERSION.SDK_INT
): AdvancedMenuBottomSheetState {
  return AdvancedMenuBottomSheetState(
    displayOptions = displayOptions,
    demoItem = LCItem(
      packageName = Constants.EXAMPLE_PACKAGE,
      label = "Example",
      versionName = "2020.3.19",
      versionCode = 1120,
      installedTime = 0,
      lastUpdatedTime = 0,
      isSystem = false,
      abi = Constants.ARMV8.toShort(),
      features = 0,
      targetApi = targetApi.toShort(),
      variant = 0
    ),
    filterOptions = listOf(
      MenuOptionItem(R.string.adv_show_system_apps, AdvancedOptions.SHOW_SYSTEM_APPS, displayOptions),
      MenuOptionItem(
        R.string.adv_show_system_framework_apps,
        AdvancedOptions.SHOW_SYSTEM_FRAMEWORK_APPS,
        displayOptions
      ),
      MenuOptionItem(R.string.adv_show_overlays, AdvancedOptions.SHOW_OVERLAYS, displayOptions),
      MenuOptionItem(R.string.adv_show_64_bit, AdvancedOptions.SHOW_64_BIT_APPS, displayOptions),
      MenuOptionItem(R.string.adv_show_32_bit, AdvancedOptions.SHOW_32_BIT_APPS, displayOptions)
    ),
    viewOptions = listOf(
      MenuOptionItem(
        R.string.adv_show_android_version,
        AdvancedOptions.SHOW_ANDROID_VERSION,
        displayOptions
      ),
      MenuOptionItem(R.string.adv_show_target_version, AdvancedOptions.SHOW_TARGET_API, displayOptions),
      MenuOptionItem(R.string.adv_show_min_version, AdvancedOptions.SHOW_MIN_API, displayOptions),
      MenuOptionItem(R.string.adv_show_compile_version, AdvancedOptions.SHOW_COMPILE_API, displayOptions),
      MenuOptionItem(R.string.adv_tint_abi_label, AdvancedOptions.TINT_ABI_LABEL, displayOptions)
    )
  )
}
