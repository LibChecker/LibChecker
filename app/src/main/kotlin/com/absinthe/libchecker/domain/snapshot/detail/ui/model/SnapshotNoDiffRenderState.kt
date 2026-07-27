package com.absinthe.libchecker.domain.snapshot.detail.ui.model

import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource

data class SnapshotNoDiffRenderState(
  val title: SnapshotTitleRenderState,
  val mode: SnapshotNoDiffMode
)

enum class SnapshotNoDiffMode {
  New,
  Deleted,
  NothingChanged,
  PackageChanges
}

data class SnapshotNoDiffTitleIconRenderState(
  val iconSource: SnapshotPackageIconSource?,
  val opensDetailOnClick: Boolean
) {
  companion object {
    val Fallback = SnapshotNoDiffTitleIconRenderState(
      iconSource = null,
      opensDetailOnClick = false
    )
  }
}

fun SnapshotDiffItem.toSnapshotNoDiffRenderState(
  title: SnapshotTitleRenderState
): SnapshotNoDiffRenderState? {
  val mode = when {
    newInstalled -> SnapshotNoDiffMode.New
    deleted -> SnapshotNoDiffMode.Deleted
    isNothingChanged() -> SnapshotNoDiffMode.NothingChanged
    hasOnlyPackageStatsChanges() -> SnapshotNoDiffMode.PackageChanges
    else -> return null
  }
  return SnapshotNoDiffRenderState(
    title = title,
    mode = mode
  )
}

fun SnapshotDiffItem.hasOnlyPackageStatsChanges(): Boolean {
  val hasPackageStatsChanges =
    dexInfoDiff.hasChanged() ||
      resourcesSizeDiff.hasChanged() ||
      resourceInfoDiff.hasChanged()
  val hasComponentChanges = listOf(
    nativeLibsDiff,
    servicesDiff,
    activitiesDiff,
    receiversDiff,
    providersDiff,
    permissionsDiff,
    metadataDiff
  ).any { it.hasChanged() }
  return hasPackageStatsChanges && !hasComponentChanges
}

private fun <T> SnapshotDiffItem.DiffNode<T>.hasChanged(): Boolean {
  return new != null && new != old
}

fun SnapshotPackageIconSource?.toSnapshotNoDiffTitleIconRenderState(): SnapshotNoDiffTitleIconRenderState {
  return SnapshotNoDiffTitleIconRenderState(
    iconSource = this,
    opensDetailOnClick = this is SnapshotPackageIconSource.InstalledPackage
  )
}
