package com.absinthe.libchecker.domain.app.list.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AppListItemMetadataDisplayTest {

  @Test
  fun createsMetadataDisplayFromViewState() {
    val viewState = AppListItemViewState(
      packageInfo = null,
      isPackageMissing = false,
      versionInfo = "2.5.5 (2759)",
      abiInfo = "arm64-v8a, Target: 37",
      accessibilityAbiInfo = "arm64-v8a, Target: 37",
      useDetachedAbiBadges = true,
      abiBadgeRes = 1,
      largeAbiBadgeRes = 2,
      isAbiBadge64Bit = true,
      showMultiArchBadge = true,
      tintAbiLabels = true,
      packageBadge = AppListItemViewState.PackageBadge.Harmony
    )

    val display = AppListItemMetadataDisplay.create(viewState)

    assertEquals("2.5.5 (2759)", display.versionInfo)
    assertSame(viewState.abiInfo, display.abiInfo)
    assertTrue(display.useDetachedAbiBadges)
    assertEquals(1, display.abiBadgeRes)
    assertEquals(2, display.largeAbiBadgeRes)
    assertTrue(display.isAbiBadge64Bit)
    assertTrue(display.showMultiArchBadge)
    assertTrue(display.tintAbiLabels)
    assertEquals(AppListItemMetadataDisplay.PackageBadge.Harmony, display.packageBadge)
  }

  @Test
  fun mapsFrozenAndMissingPackageBadge() {
    assertEquals(
      AppListItemMetadataDisplay.PackageBadge.Frozen,
      AppListItemMetadataDisplay.create(
        newViewState(packageBadge = AppListItemViewState.PackageBadge.Frozen)
      ).packageBadge
    )
    assertNull(
      AppListItemMetadataDisplay.create(
        newViewState(packageBadge = null)
      ).packageBadge
    )
  }

  private fun newViewState(
    packageBadge: AppListItemViewState.PackageBadge?
  ): AppListItemViewState {
    return AppListItemViewState(
      packageInfo = null,
      isPackageMissing = false,
      versionInfo = "",
      abiInfo = "",
      accessibilityAbiInfo = "",
      useDetachedAbiBadges = false,
      abiBadgeRes = 0,
      largeAbiBadgeRes = 0,
      isAbiBadge64Bit = false,
      showMultiArchBadge = false,
      tintAbiLabels = false,
      packageBadge = packageBadge
    )
  }
}
