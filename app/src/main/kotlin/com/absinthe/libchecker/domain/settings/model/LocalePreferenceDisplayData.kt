package com.absinthe.libchecker.domain.settings.model

data class LocalePreferenceDisplayData(
  val entries: List<LocalePreferenceEntry>,
  val summary: LocalePreferenceSummary
)

data class LocalePreferenceEntry(
  val index: Int,
  val label: String,
  val selected: Boolean
)

sealed interface LocalePreferenceSummary {
  data object FollowSystem : LocalePreferenceSummary
  data class LocaleName(val name: String) : LocalePreferenceSummary
  data object Unchanged : LocalePreferenceSummary
}
