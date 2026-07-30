package com.absinthe.libchecker.ui.preference.model

data class PreferenceItemRenderState(
  val preferenceKey: String?,
  val title: String?,
  val summary: String?,
  val toggleChecked: Boolean?,
  val showChevron: Boolean,
  val badgeDescription: String?,
  val groupPosition: PreferenceItemGroupPosition,
  val inlineControl: PreferenceInlineControl? = null,
  val expanded: Boolean = false
)

sealed interface PreferenceInlineControl {

  data class DraggableChoice(
    val entries: List<String>,
    val entryValues: List<String>,
    val selectedValue: String?,
    val deferSelectionUntilAnimationEnd: Boolean = false
  ) : PreferenceInlineControl {
    init {
      require(entries.size == entryValues.size)
      require(entryValues.isNotEmpty())
    }
  }

  data class IconSegmentedChoice(
    val accessibilityLabels: List<String>,
    val entryValues: List<String>,
    val iconResIds: List<Int>,
    val selectedValue: String?,
    val deferSelectionUntilAnimationEnd: Boolean = false
  ) : PreferenceInlineControl {
    init {
      require(accessibilityLabels.size == entryValues.size)
      require(iconResIds.size == entryValues.size)
      require(entryValues.isNotEmpty())
    }
  }

  data class Range(
    val value: Int,
    val valueFrom: Int,
    val valueTo: Int
  ) : PreferenceInlineControl {
    init {
      require(valueFrom <= valueTo)
      require(value in valueFrom..valueTo)
    }
  }
}

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
