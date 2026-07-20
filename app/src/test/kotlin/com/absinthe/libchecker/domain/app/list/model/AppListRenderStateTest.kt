package com.absinthe.libchecker.domain.app.list.model

import android.content.pm.PackageInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AppListRenderStateTest {

  @Test
  fun `merges prepared rows without losing shared render inputs`() {
    val first = itemViewState("1.0")
    val second = itemViewState("2.0")
    val packageInfo = PackageInfo()
    val initial = AppListRenderState(
      iconPackageInfos = mapOf("second" to packageInfo),
      itemViewStates = mapOf("first" to first),
      fallbackDisplayOptions = 7,
      highlightText = "sample"
    )

    val merged = initial.mergeItemViewStates(mapOf("second" to second))

    assertEquals(listOf("first", "second"), merged.itemViewStates.keys.toList())
    assertSame(first, merged.itemViewStates["first"])
    assertSame(second, merged.itemViewStates["second"])
    assertSame(packageInfo, merged.iconPackageInfos["second"])
    assertEquals(7, merged.fallbackDisplayOptions)
    assertEquals("sample", merged.highlightText)
  }

  @Test
  fun `empty row merge reuses render state`() {
    val state = AppListRenderState(highlightText = "sample")

    assertSame(state, state.mergeItemViewStates(emptyMap()))
  }

  private fun itemViewState(versionInfo: String): AppListItemViewState {
    return AppListItemViewState(
      packageInfo = null,
      isPackageMissing = false,
      versionInfo = versionInfo,
      abiInfo = "arm64-v8a",
      accessibilityAbiInfo = "arm64-v8a",
      useDetachedAbiBadges = false,
      abiBadgeRes = 0,
      largeAbiBadgeRes = 0,
      isAbiBadge64Bit = true,
      showMultiArchBadge = false,
      tintAbiLabels = false,
      packageBadge = null
    )
  }
}
