package com.absinthe.libchecker.domain.app.detail.model

import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.rulesbundle.Rule

data class LibStringComponentItemDisplay(
  override val name: LibStringItemNameDisplay,
  val rule: Rule?,
  val processName: String?,
  val processIndicatorColor: Int?,
  override val contentDescription: String
) : LibStringItemDisplay {

  val showRuleChip: Boolean = rule != null
  val ruleLabel: String? = rule?.label

  companion object {
    fun create(
      item: LibStringItemChip,
      @LibType type: Int,
      itemDisplayOptions: Int,
      processMode: Boolean,
      processIndicatorColor: Int? = null
    ): LibStringComponentItemDisplay {
      val markedRule = item.rule.takeIf {
        itemDisplayOptions.isOptionEnabled(AdvancedOptions.SHOW_MARKED_LIB)
      }
      val ruleLabel = markedRule?.label
      val processName = item.item.process
        .takeIf { processMode }
        ?.takeIf(String::isNotEmpty)
      val name = LibStringItemNameDisplay.create(
        item = item.item,
        type = type,
        itemDisplayOptions = itemDisplayOptions
      )

      return LibStringComponentItemDisplay(
        name = name,
        rule = markedRule,
        processName = processName,
        processIndicatorColor = processIndicatorColor.takeIf { processName != null },
        contentDescription = buildLibStringItemDescription(
          name.text,
          ruleLabel,
          processName
        )
      )
    }
  }
}

private fun Int.isOptionEnabled(option: Int): Boolean {
  return (this and option) > 0
}
