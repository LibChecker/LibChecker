package com.absinthe.libchecker.domain.settings

import java.util.Locale

class BuildLocalePreferenceDataUseCase(
  private val appearanceSettingsRepository: AppearanceSettingsRepository
) {

  operator fun invoke(
    entries: List<CharSequence>,
    entryValues: List<CharSequence>,
    selectedTag: String?
  ): LocalePreferenceDisplayData {
    val selectedIndex = entryValues.indexOfFirst { it.toString() == selectedTag }
    val userLocale = appearanceSettingsRepository.currentLocale
    val displayEntries = buildList {
      for (i in 1 until entries.size) {
        val locale = Locale.forLanguageTag(entries[i].toString())
        val localeName = locale.displayNameFor(locale)
        val localeNameUser = locale.displayNameFor(userLocale)
        add(
          LocalePreferenceEntry(
            index = i,
            label = if (selectedIndex != i) {
              "$localeName - $localeNameUser"
            } else {
              localeNameUser
            },
            selected = selectedIndex == i
          )
        )
      }
    }

    val summary = when {
      selectedTag.isNullOrEmpty() || selectedTag == SelectLocaleUseCase.FOLLOW_SYSTEM -> {
        LocalePreferenceSummary.FollowSystem
      }

      selectedIndex != -1 -> {
        LocalePreferenceSummary.LocaleName(displayEntries[selectedIndex - 1].label)
      }

      else -> LocalePreferenceSummary.Unchanged
    }

    return LocalePreferenceDisplayData(
      entries = displayEntries,
      summary = summary
    )
  }

  private fun Locale.displayNameFor(displayLocale: Locale): String {
    return if (script.isNotEmpty()) {
      getDisplayScript(displayLocale)
    } else {
      getDisplayName(displayLocale)
    }
  }
}

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
