package com.absinthe.libchecker.domain.statistics.chart.ui

import com.absinthe.libchecker.domain.statistics.chart.model.StatisticConditionSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticFacetSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticIconRenderMode
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticIconSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticIconTintRole
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticTitleSpec
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatisticPresentationTest {

  @Test
  fun `infers simplified Chinese script from regional locale`() {
    val translations = mapOf(
      "en" to "Other apps",
      "zh-Hans" to "其他应用"
    )

    val result = resolveStatisticTranslation(
      translations = translations,
      locale = Locale.SIMPLIFIED_CHINESE
    )

    assertEquals("其他应用", result)
  }

  @Test
  fun `infers traditional Chinese script from regional locale`() {
    val translations = mapOf(
      "en" to "Other apps",
      "zh-Hant" to "其他應用程式"
    )

    val result = resolveStatisticTranslation(
      translations = translations,
      locale = Locale.TRADITIONAL_CHINESE
    )

    assertEquals("其他應用程式", result)
  }

  @Test
  fun `uses short facet title for summaries and falls back to the full title`() {
    val fullTitle = StatisticTitleSpec(translations = mapOf("en" to "VoIP Service Kit"))
    val shortTitle = StatisticTitleSpec(translations = mapOf("en" to "VoIP"))
    val facet = StatisticFacetSpec(
      id = "voip-service-kit",
      title = fullTitle,
      shortTitle = shortTitle,
      condition = StatisticConditionSpec()
    )

    assertEquals(shortTitle, facet.summaryTitle)
    assertEquals(fullTitle, facet.copy(shortTitle = null).summaryTitle)
  }

  @Test
  fun `uses the declared tint role only for monochrome icons`() {
    val icon = StatisticIconSpec(
      renderMode = StatisticIconRenderMode.MONOCHROME,
      tintRole = StatisticIconTintRole.ON_SURFACE
    )

    assertEquals(StatisticIconTintRole.ON_SURFACE, icon.monochromeTintRole)
    assertNull(icon.copy(renderMode = StatisticIconRenderMode.ORIGINAL).monochromeTintRole)
  }

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
