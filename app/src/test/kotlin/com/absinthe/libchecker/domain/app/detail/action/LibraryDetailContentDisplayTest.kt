package com.absinthe.libchecker.domain.app.detail.action

import com.absinthe.libchecker.api.bean.LibDetailBean
import com.absinthe.libchecker.domain.app.detail.model.DetailInfoTextStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryDetailContentDisplayTest {

  @Test
  fun `builds every locale and selects the preferred locale`() {
    val display = checkNotNull(
      buildLibraryDetailContentDisplay(
        detail = LibDetailBean(
          data = listOf(
            localizedDetail(locale = "en", label = "English label"),
            localizedDetail(locale = "zh-Hans", label = "Chinese label")
          ),
          uuid = "test"
        ),
        repoUpdatedTime = "2026-07-11 12:00:00",
        preferredLocale = "zh-Hans",
        localeNameResolver = { "locale:$it" }
      )
    )

    assertEquals("zh-Hans", display.selectedLocaleTag)
    assertEquals(listOf("locale:en", "locale:zh-Hans"), display.locales.map { it.localeName })
    assertEquals(6, display.locales.first().items.size)
    assertEquals("English label", display.locales.first().items[0].text)
    assertEquals(DetailInfoTextStyle.TITLE, display.locales.first().items[0].textStyle)
    assertEquals("https://github.com/example/library", display.locales.first().items[4].linkUrl)
    assertEquals("2026-07-11 12:00:00", display.locales.first().items[5].text)
  }

  @Test
  fun `falls back to the first locale when the preference is absent`() {
    val display = checkNotNull(
      buildLibraryDetailContentDisplay(
        detail = LibDetailBean(
          data = listOf(
            localizedDetail(locale = "en", label = "English label"),
            localizedDetail(locale = "ja", label = "Japanese label")
          ),
          uuid = "test"
        ),
        repoUpdatedTime = null,
        preferredLocale = "zh-Hans"
      )
    )

    assertEquals("en", display.selectedLocaleTag)
    assertEquals(5, display.locales.first().items.size)
  }

  @Test
  fun `returns no content when the response has no locales`() {
    assertNull(
      buildLibraryDetailContentDisplay(
        detail = LibDetailBean(data = emptyList(), uuid = "test"),
        repoUpdatedTime = null,
        preferredLocale = "en"
      )
    )
  }

  private fun localizedDetail(locale: String, label: String): LibDetailBean.Data {
    return LibDetailBean.Data(
      locale = locale,
      data = LibDetailBean.DataBean(
        label = label,
        dev_team = "Example team",
        rule_contributors = listOf("Alice", "Bob"),
        description = "Example description",
        source_link = "https://github.com/example/library"
      )
    )
  }
}
