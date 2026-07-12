package com.absinthe.libchecker.domain.app.detail.model

data class DetailFeatureItem(
  val item: FeatureItem,
  val position: Int? = null
)

data class DetailFeatureListState(
  val items: List<FeatureItem> = emptyList(),
  val isLoading: Boolean = false
) {
  fun withItem(featureItem: DetailFeatureItem): DetailFeatureListState {
    val insertionIndex = featureItem.position?.coerceIn(0, items.size) ?: items.size
    return copy(
      items = items.toMutableList().apply {
        add(insertionIndex, featureItem.item)
      }
    )
  }
}
