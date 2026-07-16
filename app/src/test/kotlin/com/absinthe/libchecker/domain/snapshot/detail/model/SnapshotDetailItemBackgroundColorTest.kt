package com.absinthe.libchecker.domain.snapshot.detail.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotDetailItemBackgroundColorTest {

  @Test
  fun appliesDarkModeAlphaToStatusColor() {
    assertEquals(
      0xBF123456.toInt(),
      buildSnapshotDetailItemBackgroundColor(
        baseColor = 0xAA123456.toInt(),
        darkMode = true
      )
    )
  }

  @Test
  fun appliesLightModeAlphaToStatusColor() {
    assertEquals(
      0xF2123456.toInt(),
      buildSnapshotDetailItemBackgroundColor(
        baseColor = 0xAA123456.toInt(),
        darkMode = false
      )
    )
  }
}
