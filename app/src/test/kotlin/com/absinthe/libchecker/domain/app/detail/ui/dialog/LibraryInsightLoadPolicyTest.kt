package com.absinthe.libchecker.domain.app.detail.ui.dialog

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryInsightLoadPolicyTest {

  @Test
  fun `does not load insight outside app detail even when package info remains available`() {
    assertFalse(
      shouldLoadLibraryInsight(
        isEnabled = false,
        isPackageInfoAvailable = true
      )
    )
  }

  @Test
  fun `does not load insight before target package is available`() {
    assertFalse(
      shouldLoadLibraryInsight(
        isEnabled = true,
        isPackageInfoAvailable = false
      )
    )
  }

  @Test
  fun `loads insight for app detail with an available target package`() {
    assertTrue(
      shouldLoadLibraryInsight(
        isEnabled = true,
        isPackageInfoAvailable = true
      )
    )
  }
}
