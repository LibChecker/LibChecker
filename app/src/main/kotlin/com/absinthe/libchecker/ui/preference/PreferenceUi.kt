package com.absinthe.libchecker.ui.preference

import android.annotation.SuppressLint
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceGroupAdapter
import androidx.preference.SwitchPreferenceCompat
import androidx.preference.TwoStatePreference
import com.absinthe.libchecker.R
import com.absinthe.libchecker.ui.preference.model.PreferenceItemGroupPosition
import com.absinthe.libchecker.ui.preference.model.PreferenceItemRenderState

fun Preference.applyM3eLayoutResources() {
  when (this) {
    is PreferenceCategory -> {
      layoutResource = R.layout.preference_category_m3e
      isIconSpaceReserved = false
    }

    is SwitchPreferenceCompat -> {
      layoutResource = R.layout.preference_m3e
      widgetLayoutResource = R.layout.preference_widget_material_switch
      isIconSpaceReserved = true
    }

    else -> {
      layoutResource = R.layout.preference_m3e
      isIconSpaceReserved = true
    }
  }

  if (this is PreferenceGroup) {
    for (index in 0 until preferenceCount) {
      getPreference(index).applyM3eLayoutResources()
    }
  }
}

@SuppressLint("RestrictedApi")
fun PreferenceGroupAdapter.buildPreferenceItemRenderState(
  position: Int,
  showChevron: (Preference) -> Boolean = { false },
  badgeDescription: (Preference) -> String? = { null }
): PreferenceItemRenderState? {
  if (position !in 0 until itemCount) {
    return null
  }
  val preference = getItem(position) ?: return null
  if (preference is PreferenceCategory) {
    return null
  }
  val hasPreviousItem = position > 0 && getItem(position - 1).isItemPreference()
  val hasNextItem = position < itemCount - 1 && getItem(position + 1).isItemPreference()
  return PreferenceItemRenderState(
    title = preference.title?.toString(),
    summary = preference.summary?.toString(),
    toggleChecked = (preference as? TwoStatePreference)?.isChecked,
    showChevron = showChevron(preference),
    badgeDescription = badgeDescription(preference),
    groupPosition = PreferenceItemGroupPosition.from(hasPreviousItem, hasNextItem)
  )
}

@SuppressLint("RestrictedApi")
fun PreferenceGroupAdapter.findPreferencePosition(preference: Preference): Int? {
  return (0 until itemCount).firstOrNull { getItem(it) == preference }
}

private fun Preference?.isItemPreference(): Boolean {
  return this != null && this !is PreferenceCategory
}
