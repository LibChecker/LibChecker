package com.absinthe.libchecker.domain.settings

import java.util.Locale

interface AppearanceSettingsRepository {
  var darkMode: String
  var localeTag: String?
  val currentLocale: Locale
}
