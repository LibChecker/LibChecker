package com.absinthe.libchecker.domain.app.list.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppListItemDescriptionTest {

  @Test
  fun buildsDescriptionFromRenderableTextParts() {
    assertEquals(
      "LibChecker, com.absinthe.libchecker, 2.5.5 (2757), arm64-v8a, Target: 37",
      buildAppListItemDescription(
        "LibChecker",
        "com.absinthe.libchecker",
        "2.5.5 (2757)",
        "arm64-v8a, Target: 37"
      )
    )
  }

  @Test
  fun skipsBlankDescriptionParts() {
    assertEquals(
      "LibChecker, arm64-v8a",
      buildAppListItemDescription(
        "LibChecker",
        " ",
        null,
        "arm64-v8a"
      )
    )
  }
}
