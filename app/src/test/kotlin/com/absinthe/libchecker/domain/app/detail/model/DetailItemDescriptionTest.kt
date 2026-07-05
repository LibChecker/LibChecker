package com.absinthe.libchecker.domain.app.detail.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DetailItemDescriptionTest {

  @Test
  fun buildsDescriptionFromVisibleTextParts() {
    assertEquals(
      "Label, content",
      buildDetailItemDescription(
        "Label",
        "content"
      )
    )
  }

  @Test
  fun skipsBlankDescriptionParts() {
    assertEquals(
      "Label",
      buildDetailItemDescription(
        "Label",
        "",
        " ",
        null
      )
    )
  }
}
