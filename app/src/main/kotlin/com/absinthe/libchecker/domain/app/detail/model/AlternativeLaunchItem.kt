package com.absinthe.libchecker.domain.app.detail.model

data class AlternativeLaunchItem(
  val label: String,
  val className: String,
  val contentDescription: String = buildDetailItemDescription(label, className)
)

sealed interface AlternativeLaunchAction {
  data class OpenActivity(
    val item: AlternativeLaunchItem
  ) : AlternativeLaunchAction
}
