package com.absinthe.libchecker.domain.snapshot.list.model

import com.absinthe.libchecker.domain.snapshot.SnapshotListDisplayOptions
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SnapshotListRenderStateTest {

  @Test
  fun `state carries all list rendering inputs together`() {
    val displayOptions = SnapshotListDisplayOptions(
      highlightDiffs = true,
      showUpdateTime = true,
      tintAbiLabels = true
    )
    val iconSource = SnapshotPackageIconSource.Fallback

    val state = SnapshotListRenderState(
      displayOptions = displayOptions,
      packageIconSources = mapOf("sample.package" to iconSource),
      apexPackageNames = setOf("com.android.apex"),
      highlightText = "sample"
    )

    assertSame(displayOptions, state.displayOptions)
    assertSame(iconSource, state.packageIconSources["sample.package"])
    assertEquals(setOf("com.android.apex"), state.apexPackageNames)
    assertEquals("sample", state.highlightText)
  }
}
