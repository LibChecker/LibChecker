package com.absinthe.libchecker.domain.snapshot.detail.ui.model

import android.content.pm.PackageInfo
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotNoDiffRenderStateTest {

  @Test
  fun mapsNewInstalledItemToNewMode() {
    val title = titleRenderState()
    val item = snapshotDiffItem().apply {
      newInstalled = true
      added = 1
    }

    assertEquals(
      SnapshotNoDiffRenderState(
        title = title,
        mode = SnapshotNoDiffMode.New
      ),
      item.toSnapshotNoDiffRenderState(title)
    )
  }

  @Test
  fun mapsDeletedItemToDeletedMode() {
    val title = titleRenderState()
    val item = snapshotDiffItem().apply {
      deleted = true
      removed = 1
    }

    assertEquals(
      SnapshotNoDiffRenderState(
        title = title,
        mode = SnapshotNoDiffMode.Deleted
      ),
      item.toSnapshotNoDiffRenderState(title)
    )
  }

  @Test
  fun mapsNothingChangedItemToNothingChangedMode() {
    val title = titleRenderState()

    assertEquals(
      SnapshotNoDiffRenderState(
        title = title,
        mode = SnapshotNoDiffMode.NothingChanged
      ),
      snapshotDiffItem().toSnapshotNoDiffRenderState(title)
    )
  }

  @Test
  fun ignoresChangedItemThatNeedsDetailDiff() {
    val item = snapshotDiffItem().apply {
      changed = 1
    }

    assertNull(item.toSnapshotNoDiffRenderState(titleRenderState()))
  }

  @Test
  fun disablesDetailClickForFallbackIconSource() {
    val state = SnapshotPackageIconSource.Fallback.toSnapshotNoDiffTitleIconRenderState()

    assertEquals(SnapshotPackageIconSource.Fallback, state.iconSource)
    assertFalse(state.opensDetailOnClick)
  }

  @Test
  fun disablesDetailClickForMissingIconSource() {
    val state = null.toSnapshotNoDiffTitleIconRenderState()

    assertNull(state.iconSource)
    assertFalse(state.opensDetailOnClick)
  }

  @Test
  fun enablesDetailClickForInstalledPackageIconSource() {
    val iconSource = SnapshotPackageIconSource.InstalledPackage(PackageInfo())
    val state = iconSource.toSnapshotNoDiffTitleIconRenderState()

    assertEquals(iconSource, state.iconSource)
    assertTrue(state.opensDetailOnClick)
  }

  private fun titleRenderState(): SnapshotTitleRenderState {
    return SnapshotTitleRenderState(
      appName = "X",
      iconContentDescription = "X",
      packageName = "com.twitter.android",
      versionInfo = "12.5.0",
      packageSize = null,
      apis = "Target: 35",
      summary = null,
      copyPrimaryText = false
    )
  }

  private fun snapshotDiffItem(): SnapshotDiffItem {
    return SnapshotDiffItem(
      packageName = "com.twitter.android",
      updateTime = 0L,
      labelDiff = SnapshotDiffItem.DiffNode("X"),
      versionNameDiff = SnapshotDiffItem.DiffNode("12.5.0"),
      versionCodeDiff = SnapshotDiffItem.DiffNode(312050000L),
      abiDiff = SnapshotDiffItem.DiffNode(0),
      targetApiDiff = SnapshotDiffItem.DiffNode(35),
      compileSdkDiff = SnapshotDiffItem.DiffNode(36),
      minSdkDiff = SnapshotDiffItem.DiffNode(28),
      nativeLibsDiff = SnapshotDiffItem.DiffNode(""),
      servicesDiff = SnapshotDiffItem.DiffNode(""),
      activitiesDiff = SnapshotDiffItem.DiffNode(""),
      receiversDiff = SnapshotDiffItem.DiffNode(""),
      providersDiff = SnapshotDiffItem.DiffNode(""),
      permissionsDiff = SnapshotDiffItem.DiffNode(""),
      metadataDiff = SnapshotDiffItem.DiffNode(""),
      packageSizeDiff = SnapshotDiffItem.DiffNode(0L)
    )
  }
}
