package com.absinthe.libchecker.domain.rules

import java.util.Locale

internal fun defaultRuleLanguage(locale: Locale): String = if (locale.language == "zh" || locale.country in setOf("CN", "HK", "MO", "TW")) "zh-Hans" else "en"
