package com.absinthe.libchecker.domain.statistics.chart.model

import androidx.annotation.DrawableRes
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.list.model.AppListItemViewState

data class ClassifyDialogState(
  val title: String,
  val items: List<LCItem>,
  val itemViewStates: Map<String, AppListItemViewState>,
  val itemChips: Map<String, List<String>> = emptyMap(),
  val androidVersion: AndroidVersionLabelDisplayData?
)

data class AndroidVersionLabelDisplayData(
  @DrawableRes val iconRes: Int?,
  val text: String,
  val contentDescription: String = text
)

sealed interface ClassifyDialogAction {
  data class OpenApp(
    val item: LCItem
  ) : ClassifyDialogAction
}
