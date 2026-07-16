package com.absinthe.libchecker.domain.app.detail.model

import com.absinthe.libchecker.constant.options.AdvancedOptions

data class LibStringRenderState(
  val itemDisplayOptions: Int = AdvancedOptions.ITEM_DEFAULT_OPTIONS,
  val colorfulRuleIcon: Boolean = true,
  val highlightText: String = "",
  val processMode: Boolean = false,
  val processColors: Map<String, Int> = emptyMap(),
  val highlightPosition: Int = NO_HIGHLIGHT_POSITION
) {

  fun withHighlightPosition(position: Int): LibStringRenderState {
    return if (position < 0) this else copy(highlightPosition = position)
  }

  companion object {
    const val NO_HIGHLIGHT_POSITION = -1
  }
}

sealed interface LibStringAction {
  data class MetadataResourceClicked(
    val item: LibStringItemChip,
    val display: LibStringMetadataItemDisplay
  ) : LibStringAction
}
