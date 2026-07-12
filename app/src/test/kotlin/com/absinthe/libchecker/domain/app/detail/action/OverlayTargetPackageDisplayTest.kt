package com.absinthe.libchecker.domain.app.detail.action

import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.detail.model.OverlayTargetPackageDisplay
import com.absinthe.libchecker.domain.app.detail.related.RelatedAppDisplayData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayTargetPackageDisplayTest {

  @Test
  fun `builds empty target when overlay target is missing`() {
    assertEquals(
      OverlayTargetPackageDisplay.Empty,
      buildOverlayTargetPackageDisplay(testItem(), targetPackageName = null, targetApp = null)
    )
  }

  @Test
  fun `keeps target package name when related app is unavailable`() {
    assertEquals(
      OverlayTargetPackageDisplay.PackageName("target.package"),
      buildOverlayTargetPackageDisplay(
        testItem(),
        targetPackageName = "target.package",
        targetApp = null
      )
    )
  }

  @Test
  fun `builds related target and preserves Harmony badge rule`() {
    val relatedApp = relatedApp()
    val standard = buildOverlayTargetPackageDisplay(
      item = testItem(),
      targetPackageName = relatedApp.packageName,
      targetApp = relatedApp
    ) as OverlayTargetPackageDisplay.RelatedApp
    val harmony = buildOverlayTargetPackageDisplay(
      item = testItem(variant = Constants.VARIANT_HAP),
      targetPackageName = relatedApp.packageName,
      targetApp = relatedApp
    ) as OverlayTargetPackageDisplay.RelatedApp

    assertSame(relatedApp, standard.data)
    assertFalse(standard.showHarmonyBadge)
    assertSame(relatedApp, harmony.data)
    assertTrue(harmony.showHarmonyBadge)
  }

  private fun relatedApp(): RelatedAppDisplayData {
    val item = testItem(packageName = "target.package")
    return RelatedAppDisplayData(
      item = item,
      packageInfo = null,
      packageName = item.packageName,
      label = "Target",
      versionInfo = "1.0 (1)",
      abiInfo = "arm64-v8a",
      abiBadgeRes = null,
      isHarmony = false
    )
  }

  private fun testItem(
    packageName: String = "overlay.package",
    variant: Short = 0
  ): LCItem {
    return LCItem(
      packageName = packageName,
      label = "Overlay",
      versionName = "1.0",
      versionCode = 1,
      installedTime = 0,
      lastUpdatedTime = 0,
      isSystem = false,
      abi = 0,
      features = 0,
      targetApi = 35,
      variant = variant
    )
  }
}
