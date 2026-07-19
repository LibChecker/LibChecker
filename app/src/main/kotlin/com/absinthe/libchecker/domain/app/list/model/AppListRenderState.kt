package com.absinthe.libchecker.domain.app.list.model

import com.absinthe.libchecker.constant.options.AdvancedOptions

data class AppListRenderState(
  val itemViewStates: Map<String, AppListItemViewState> = emptyMap(),
  val itemChips: Map<String, List<String>> = emptyMap(),
  val fallbackDisplayOptions: Int = AdvancedOptions.DEFAULT_OPTIONS,
  val highlightText: String = ""
) {

  fun mergeItemViewStates(
    itemViewStates: Map<String, AppListItemViewState>
  ): AppListRenderState {
    if (itemViewStates.isEmpty()) {
      return this
    }
    return copy(itemViewStates = this.itemViewStates + itemViewStates)
  }
}
