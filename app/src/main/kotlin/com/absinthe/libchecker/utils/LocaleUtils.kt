package com.absinthe.libchecker.utils

import android.content.Context
import android.content.res.Configuration
import com.absinthe.libchecker.constant.GlobalValues
import java.util.Locale

object LocaleUtils {
  val systemLocale: Locale
    get() = Locale.getDefault()

  val defaultLocale: Locale
    get() = GlobalValues.locale

  fun wrapContext(context: Context, locale: Locale = defaultLocale): Context {
    Locale.setDefault(locale)
    val configuration = Configuration(context.resources.configuration).apply {
      setLocale(locale)
      setLayoutDirection(locale)
    }
    return context.createConfigurationContext(configuration)
  }
}
