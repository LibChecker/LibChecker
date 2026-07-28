package com.absinthe.libchecker.view.app

import org.junit.Assert.assertTrue
import org.junit.Test

class AppIconPlaceholderTest {

  @Test
  fun `placeholder resource id is resolved at runtime`() {
    assertTrue(
      "Adaptive icon placeholder resource must not be the unresolved zero ID",
      AppIconPlaceholder.resourceId != 0
    )
  }
}
