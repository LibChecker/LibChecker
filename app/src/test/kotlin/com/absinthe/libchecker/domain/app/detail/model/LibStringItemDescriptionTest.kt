package com.absinthe.libchecker.domain.app.detail.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LibStringItemDescriptionTest {

  @Test
  fun buildsDescriptionFromVisibleTextParts() {
    assertEquals(
      "libfoo.so, 123 KB, Rule label",
      buildLibStringItemDescription(
        "libfoo.so",
        "123 KB",
        "Rule label"
      )
    )
  }

  @Test
  fun skipsBlankDescriptionParts() {
    assertEquals(
      "android.permission.CAMERA, Not granted",
      buildLibStringItemDescription(
        "android.permission.CAMERA",
        " ",
        null,
        "Not granted"
      )
    )
  }
}
