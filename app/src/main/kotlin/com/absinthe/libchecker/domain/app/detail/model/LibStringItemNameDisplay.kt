package com.absinthe.libchecker.domain.app.detail.model

import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.constant.options.AdvancedOptions

data class LibStringItemNameDisplay(
  val text: String,
  val decoration: Decoration
) {

  enum class Decoration {
    Plain,
    Disabled,
    Exported
  }

  companion object {
    fun create(
      item: LibStringItem,
      @LibType type: Int,
      itemDisplayOptions: Int
    ): LibStringItemNameDisplay {
      return LibStringItemNameDisplay(
        text = item.name,
        decoration = when (item.source) {
          DISABLED -> if (
            itemDisplayOptions.isOptionEnabled(AdvancedOptions.MARK_DISABLED) ||
            type == PERMISSION
          ) {
            Decoration.Disabled
          } else {
            Decoration.Plain
          }

          EXPORTED -> if (itemDisplayOptions.isOptionEnabled(AdvancedOptions.MARK_EXPORTED)) {
            Decoration.Exported
          } else {
            Decoration.Plain
          }

          else -> Decoration.Plain
        }
      )
    }
  }
}

private fun Int.isOptionEnabled(option: Int): Boolean {
  return (this and option) > 0
}
