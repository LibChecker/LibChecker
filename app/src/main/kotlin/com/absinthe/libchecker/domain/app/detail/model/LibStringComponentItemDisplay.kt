package com.absinthe.libchecker.domain.app.detail.model

import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.constant.options.AdvancedOptions

data class LibStringComponentItemDisplay(
  val name: LibStringItemNameDisplay,
  val showRuleChip: Boolean,
  val ruleLabel: String?,
  val processName: String?,
  val contentDescription: String
) {

  companion object {
    fun create(
      item: LibStringItemChip,
      @LibType type: Int,
      itemDisplayOptions: Int,
      processMode: Boolean
    ): LibStringComponentItemDisplay {
      val markedRule = item.rule.takeIf {
        itemDisplayOptions.isOptionEnabled(AdvancedOptions.SHOW_MARKED_LIB)
      }
      val showRuleChip = markedRule != null
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
        showRuleChip = showRuleChip,
        ruleLabel = ruleLabel,
        processName = processName,
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
