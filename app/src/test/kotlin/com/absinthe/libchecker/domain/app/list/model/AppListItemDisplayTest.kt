package com.absinthe.libchecker.domain.app.list.model

import android.content.pm.PackageInfo
import com.absinthe.libchecker.constant.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AppListItemDisplayTest {

  @Test
  fun createsRenderableDisplayForAppListItem() {
    val packageInfo = PackageInfo()
    val viewState = newViewState(
      packageInfo = packageInfo,
      versionInfo = "2.5.5 (2761)",
      packageBadge = AppListItemViewState.PackageBadge.Frozen
    )

    val display = AppListItemDisplay.create(
      label = "LibChecker",
      packageName = "com.absinthe.libchecker",
      viewState = viewState,
      showMissingPackageStrikeThrough = true
    )

    assertTrue(display.icon.usePackageIcon)
    assertSame(packageInfo, display.icon.packageInfo)
    assertEquals("LibChecker", display.identity.label)
    assertEquals("com.absinthe.libchecker", display.identity.packageName)
    assertTrue(display.identity.showMissingPackageStrikeThrough)
    assertEquals(
      "LibChecker, com.absinthe.libchecker, 2.5.5 (2761), arm64-v8a, Target: 37",
      display.identity.contentDescription
    )
    assertEquals("2.5.5 (2761)", display.metadata.versionInfo)
    assertEquals(AppListItemMetadataDisplay.PackageBadge.Frozen, display.metadata.packageBadge)
  }

  @Test
  fun keepsExampleItemOnPlaceholderIcon() {
    val display = AppListItemDisplay.create(
      label = "Example",
      packageName = Constants.EXAMPLE_PACKAGE,
      viewState = newViewState(packageInfo = PackageInfo()),
      showMissingPackageStrikeThrough = false
    )

    assertFalse(display.icon.usePackageIcon)
    assertFalse(display.identity.showMissingPackageStrikeThrough)
  }

  private fun newViewState(
    packageInfo: PackageInfo? = null,
    versionInfo: String = "",
    packageBadge: AppListItemViewState.PackageBadge? = null
  ): AppListItemViewState {
    return AppListItemViewState(
      packageInfo = packageInfo,
      isPackageMissing = false,
      versionInfo = versionInfo,
      abiInfo = "arm64-v8a, Target: 37",
      accessibilityAbiInfo = "arm64-v8a, Target: 37",
      useDetachedAbiBadges = false,
      abiBadgeRes = 0,
      largeAbiBadgeRes = 0,
      isAbiBadge64Bit = true,
      showMultiArchBadge = false,
      tintAbiLabels = false,
      packageBadge = packageBadge
    )
  }
}
