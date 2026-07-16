package com.absinthe.libchecker.domain.app.detail.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppBundleItemTest {

  @Test
  fun buildsDescriptionFromVisibleTextParts() {
    assertEquals(
      "split_config.arm64_v8a.apk, 123 KB",
      buildAppBundleItemDescription(
        "split_config.arm64_v8a.apk",
        "123 KB"
      )
    )
  }

  @Test
  fun skipsBlankDescriptionParts() {
    assertEquals(
      "split_config.xxhdpi.apk",
      buildAppBundleItemDescription(
        "split_config.xxhdpi.apk",
        " ",
        null
      )
    )
  }
}
