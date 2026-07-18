package com.absinthe.libchecker.domain.statistics.chart.ui

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticPresentationTest {

  @Test
  fun `resolves language script translation before regional fallback`() {
    val translations = mapOf(
      "en" to "Other apps",
      "zh-Hans" to "其他应用"
    )

    val result = resolveStatisticTranslation(
      translations = translations,
      locale = Locale.forLanguageTag("zh-Hans-CN")
    )

    assertEquals("其他应用", result)
  }
}
