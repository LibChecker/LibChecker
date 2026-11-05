package com.absinthe.libchecker.domain.rules

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class RuleLanguageTest {
  @Test fun chineseLanguageOrRegionUsesChinese() {
    for (tag in listOf("zh-CN", "zh-TW", "zh-HK", "zh-Hans", "zh-Hant", "zh-US", "en-CN", "en-HK", "en-MO", "en-TW")) {
      assertEquals(tag, "zh-Hans", defaultRuleLanguage(Locale.forLanguageTag(tag)))
    }
  }

  @Test fun otherLocalesUseEnglish() {
    for (tag in listOf("en-US", "en-GB", "ja-JP", "de-DE", "en-SG", "und")) {
      assertEquals(tag, "en", defaultRuleLanguage(Locale.forLanguageTag(tag)))
    }
  }
}
