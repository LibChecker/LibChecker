package com.absinthe.libchecker.domain.app.list.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppListItemIdentityTextTest {

  @Test
  fun buildsIdentityTextWithContentDescription() {
    val identityText = AppListItemIdentityText.create(
      label = "LibChecker",
      packageName = "com.absinthe.libchecker",
      versionInfo = "2.5.5 (2758)",
      accessibilityAbiInfo = "arm64-v8a, Target: 37",
      showMissingPackageStrikeThrough = false
    )

    assertEquals("LibChecker", identityText.label)
    assertEquals("com.absinthe.libchecker", identityText.packageName)
    assertEquals(
      "LibChecker, com.absinthe.libchecker, 2.5.5 (2758), arm64-v8a, Target: 37",
      identityText.contentDescription
    )
    assertFalse(identityText.showMissingPackageStrikeThrough)
  }

  @Test
  fun keepsMissingPackageDecorationSeparateFromContentDescription() {
    val identityText = AppListItemIdentityText.create(
      label = "Removed app",
      packageName = "com.example.removed",
      versionInfo = "1.0 (1)",
      accessibilityAbiInfo = "arm64-v8a",
      showMissingPackageStrikeThrough = true
    )

    assertEquals(
      "Removed app, com.example.removed, 1.0 (1), arm64-v8a",
      identityText.contentDescription
    )
    assertTrue(identityText.showMissingPackageStrikeThrough)
  }
}
