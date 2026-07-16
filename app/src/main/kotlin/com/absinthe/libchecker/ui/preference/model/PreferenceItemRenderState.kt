package com.absinthe.libchecker.ui.preference.model

data class PreferenceItemRenderState(
  val title: String?,
  val summary: String?,
  val toggleChecked: Boolean?,
  val showChevron: Boolean,
  val badgeDescription: String?,
  val groupPosition: PreferenceItemGroupPosition
)

enum class PreferenceItemGroupPosition(
  val usesOuterTopCorners: Boolean,
  val usesOuterBottomCorners: Boolean
) {
  SINGLE(usesOuterTopCorners = true, usesOuterBottomCorners = true),
  FIRST(usesOuterTopCorners = true, usesOuterBottomCorners = false),
  MIDDLE(usesOuterTopCorners = false, usesOuterBottomCorners = false),
  LAST(usesOuterTopCorners = false, usesOuterBottomCorners = true);

  companion object {
    fun from(hasPreviousItem: Boolean, hasNextItem: Boolean): PreferenceItemGroupPosition {
      return when {
        !hasPreviousItem && !hasNextItem -> SINGLE
        !hasPreviousItem -> FIRST
        !hasNextItem -> LAST
        else -> MIDDLE
      }
    }
  }
}
