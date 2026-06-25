package com.absinthe.libchecker.data.settings

import androidx.core.content.edit
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.domain.settings.AppearanceSettingsRepository
import com.absinthe.libchecker.utils.SPUtils
import java.util.Locale

class GlobalAppearanceSettingsRepository : AppearanceSettingsRepository {
  override var darkMode: String
    get() = GlobalValues.darkMode
    set(value) {
      GlobalValues.darkMode = value
    }

  override var localeTag: String?
    get() = SPUtils.sp.getString(Constants.PREF_LOCALE, null)
    set(value) {
      SPUtils.sp.edit { putString(Constants.PREF_LOCALE, value) }
    }

  override val currentLocale: Locale
    get() = GlobalValues.locale
}
