package com.absinthe.libchecker.domain.app.detail.model

import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.ui.app.MenuOptionItem
import com.absinthe.rulesbundle.Rule

data class ComponentMenuState(
  val itemDisplayOptions: Int,
  val colorfulRuleIcon: Boolean,
  val componentDemoItems: List<LibStringItemChip>,
  val componentOptions: List<MenuOptionItem>
)

fun buildComponentMenuState(options: Int, colorfulRuleIcon: Boolean, rulePackageName: String): ComponentMenuState {
  return ComponentMenuState(
    itemDisplayOptions = options,
    colorfulRuleIcon = colorfulRuleIcon,
    componentDemoItems = buildComponentDemoItems(rulePackageName),
    componentOptions = listOf(
      MenuOptionItem(R.string.adv_mark_exported, AdvancedOptions.MARK_EXPORTED, options),
      MenuOptionItem(R.string.adv_mark_disabled, AdvancedOptions.MARK_DISABLED, options),
      MenuOptionItem(R.string.adv_show_marked_lib, AdvancedOptions.SHOW_MARKED_LIB, options)
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
