package com.absinthe.libchecker.domain.settings.repository

import java.util.Locale

interface AppearanceSettingsRepository {
  var darkMode: String
  var localeTag: String?
  val currentLocale: Locale
}
