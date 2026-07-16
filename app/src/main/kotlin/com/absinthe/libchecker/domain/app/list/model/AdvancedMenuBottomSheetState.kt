package com.absinthe.libchecker.domain.app.list.model

import android.os.Build
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.detail.model.DISABLED
import com.absinthe.libchecker.domain.app.detail.model.EXPORTED
import com.absinthe.libchecker.domain.app.detail.model.LibStringItem
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip
import com.absinthe.libchecker.ui.app.MenuOptionItem
import com.absinthe.rulesbundle.Rule

data class AdvancedMenuBottomSheetState(
  val displayOptions: Int,
  val itemDisplayOptions: Int,
  val colorfulRuleIcon: Boolean,
  val demoItem: LCItem,
  val componentDemoItems: List<LibStringItemChip>,
  val filterOptions: List<MenuOptionItem>,
  val viewOptions: List<MenuOptionItem>,
  val componentOptions: List<MenuOptionItem>
)

sealed interface AdvancedMenuAction {
  data class SortChanged(
    val displayOptions: Int
  ) : AdvancedMenuAction

  data class DisplayOptionChanged(
    val item: MenuOptionItem,
    val isChecked: Boolean
  ) : AdvancedMenuAction

  data class ItemDisplayOptionChanged(
    val item: MenuOptionItem,
    val isChecked: Boolean
  ) : AdvancedMenuAction
}

fun buildAdvancedMenuBottomSheetState(
  displayOptions: Int,
  itemDisplayOptions: Int,
  colorfulRuleIcon: Boolean,
  rulePackageName: String,
  targetApi: Int = Build.VERSION.SDK_INT
): AdvancedMenuBottomSheetState {
  return AdvancedMenuBottomSheetState(
    displayOptions = displayOptions,
    itemDisplayOptions = itemDisplayOptions,
    colorfulRuleIcon = colorfulRuleIcon,
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
    componentDemoItems = buildComponentDemoItems(rulePackageName),
    filterOptions = listOf(
      menuOption(R.string.adv_show_system_apps, AdvancedOptions.SHOW_SYSTEM_APPS, displayOptions),
      menuOption(
        R.string.adv_show_system_framework_apps,
        AdvancedOptions.SHOW_SYSTEM_FRAMEWORK_APPS,
        displayOptions
      ),
      menuOption(R.string.adv_show_overlays, AdvancedOptions.SHOW_OVERLAYS, displayOptions),
      menuOption(R.string.adv_show_64_bit, AdvancedOptions.SHOW_64_BIT_APPS, displayOptions),
      menuOption(R.string.adv_show_32_bit, AdvancedOptions.SHOW_32_BIT_APPS, displayOptions)
    ),
    viewOptions = listOf(
      menuOption(
        R.string.adv_show_android_version,
        AdvancedOptions.SHOW_ANDROID_VERSION,
        displayOptions
      ),
      menuOption(R.string.adv_show_target_version, AdvancedOptions.SHOW_TARGET_API, displayOptions),
      menuOption(R.string.adv_show_min_version, AdvancedOptions.SHOW_MIN_API, displayOptions),
      menuOption(R.string.adv_show_compile_version, AdvancedOptions.SHOW_COMPILE_API, displayOptions),
      menuOption(R.string.adv_tint_abi_label, AdvancedOptions.TINT_ABI_LABEL, displayOptions)
    ),
    componentOptions = listOf(
      menuOption(R.string.adv_mark_exported, AdvancedOptions.MARK_EXPORTED, itemDisplayOptions),
      menuOption(R.string.adv_mark_disabled, AdvancedOptions.MARK_DISABLED, itemDisplayOptions),
      menuOption(R.string.adv_show_marked_lib, AdvancedOptions.SHOW_MARKED_LIB, itemDisplayOptions)
    )
  )
}

private fun buildComponentDemoItems(rulePackageName: String): List<LibStringItemChip> {
  fun rule(): Rule {
    return Rule(
      rulePackageName,
      NATIVE,
      Constants.EXAMPLE_RULE,
      R.drawable.ic_logo,
      null,
      null,
      true
    )
  }

  return listOf(
    LibStringItemChip(
      LibStringItem(
        name = Constants.EXAMPLE_EXPORTED,
        source = EXPORTED
      ),
      rule()
    ),
    LibStringItemChip(
      LibStringItem(name = Constants.EXAMPLE_NORMAL),
      rule()
    ),
    LibStringItemChip(
      LibStringItem(
        name = Constants.EXAMPLE_DISABLED,
        source = DISABLED
      ),
      rule()
    )
  )
}

private fun Int.hasOption(option: Int): Boolean = (this and option) > 0

private fun menuOption(labelRes: Int, option: Int, currentOptions: Int): MenuOptionItem {
  return MenuOptionItem(
    labelRes = labelRes,
    option = option,
    isChecked = currentOptions.hasOption(option)
  )
}
