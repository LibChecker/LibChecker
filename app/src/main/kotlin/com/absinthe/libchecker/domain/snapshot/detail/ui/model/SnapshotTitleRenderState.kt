package com.absinthe.libchecker.domain.snapshot.detail.ui.model

import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotTitleDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotTitlePackageSizeData

data class SnapshotTitleRenderState(
  val appName: CharSequence,
  val packageName: CharSequence,
  val versionInfo: CharSequence,
  val packageSize: SnapshotTitlePackageSizeRenderState?,
  val apis: CharSequence,
  val copyPrimaryText: Boolean
)

data class SnapshotTitlePackageSizeRenderState(
  val text: CharSequence,
  val breakStart: Int
)

fun SnapshotTitleDisplayData.toRenderState(
  copyPrimaryText: Boolean = true
): SnapshotTitleRenderState {
  return SnapshotTitleRenderState(
    appName = appName,
    packageName = packageName,
    versionInfo = versionInfo,
    packageSize = packageSize?.toRenderState(),
    apis = apis,
    copyPrimaryText = copyPrimaryText
  )
}

private fun SnapshotTitlePackageSizeData.toRenderState(): SnapshotTitlePackageSizeRenderState {
  return SnapshotTitlePackageSizeRenderState(
    text = text,
    breakStart = breakStart
  )
}
