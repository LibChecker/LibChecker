package com.absinthe.libchecker.domain.snapshot.detail.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotDetailReportSectionTextTest {

  @Test
  fun wrapsSectionTitleInBrackets() {
    assertEquals(
      "[Services]\n",
      buildSnapshotDetailReportSectionText("Services")
    )
  }
}
