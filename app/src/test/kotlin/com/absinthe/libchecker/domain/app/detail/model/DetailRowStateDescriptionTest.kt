package com.absinthe.libchecker.domain.app.detail.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DetailRowStateDescriptionTest {

  @Test
  fun alternativeLaunchItemBuildsContentDescription() {
    val item = AlternativeLaunchItem(
      label = "Launcher",
      className = "com.example.LauncherActivity"
    )

    assertEquals(
      "Launcher, com.example.LauncherActivity",
      item.contentDescription
    )
  }

  @Test
  fun signatureDetailItemBuildsContentDescription() {
    val item = SignatureDetailItem(
      type = "SHA-256",
      content = "ABCD"
    )

    assertEquals(
      "SHA-256, ABCD",
      item.contentDescription
    )
  }
}
