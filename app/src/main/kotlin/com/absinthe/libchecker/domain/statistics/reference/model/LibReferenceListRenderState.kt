package com.absinthe.libchecker.domain.statistics.reference.model

data class LibReferenceListRenderState(
  val highlightText: String = "",
  val colorfulRuleIcon: Boolean = true,
  val labelSuffix: String = ""
)

sealed interface LibReferenceAction {
  data class DetailIconClicked(
    val reference: LibReference
  ) : LibReferenceAction
}
