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
  NothingChanged
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
  if (!shouldShowSnapshotNoDiffPopup()) {
    return null
  }
  val mode = when {
    newInstalled -> SnapshotNoDiffMode.New
    deleted -> SnapshotNoDiffMode.Deleted
    else -> SnapshotNoDiffMode.NothingChanged
  }
  return SnapshotNoDiffRenderState(
    title = title,
    mode = mode
  )
}

internal fun SnapshotDiffItem.shouldShowSnapshotNoDiffPopup(): Boolean {
  return newInstalled ||
    deleted ||
    (
      isNothingChanged() &&
        !dexInfoDiff.hasChanged() &&
        !resourcesSizeDiff.hasChanged() &&
        !resourceInfoDiff.hasChanged()
      )
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
