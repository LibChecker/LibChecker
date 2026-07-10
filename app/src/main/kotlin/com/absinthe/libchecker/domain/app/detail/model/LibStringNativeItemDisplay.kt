package com.absinthe.libchecker.domain.app.detail.model

import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.rulesbundle.Rule

data class LibStringNativeItemDisplay(
  override val name: LibStringItemNameDisplay,
  val sizeText: String,
  val labels: List<String>,
  val rule: Rule?,
  override val contentDescription: String
) : LibStringItemDisplay {

  companion object {
    fun create(
      item: LibStringItemChip,
      itemDisplayOptions: Int
    ): LibStringNativeItemDisplay {
      val displayData = checkNotNull(item.nativeDisplayData) {
        "NativeLibraryItemDisplayData is required for native library rows."
      }
      val name = LibStringItemNameDisplay.create(
        item = item.item,
        type = NATIVE,
        itemDisplayOptions = itemDisplayOptions
      )
      val rule = item.rule.takeIf {
        itemDisplayOptions.isOptionEnabled(AdvancedOptions.SHOW_MARKED_LIB)
      }
      val labels = displayData.labels.toList()
      val secondaryText = buildString {
        append(displayData.sizeText)
        labels.forEach { label ->
          append(' ').append(label).append(' ')
        }
      }

      return LibStringNativeItemDisplay(
        name = name,
        sizeText = displayData.sizeText,
        labels = labels,
        rule = rule,
        contentDescription = buildLibStringItemDescription(
          name.text,
          secondaryText,
          rule?.label
        )
      )
    }
  }
}

private fun Int.isOptionEnabled(option: Int): Boolean {
  return (this and option) > 0
}
