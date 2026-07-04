package com.absinthe.libchecker.domain.snapshot.detail.ui.model

import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotTitleDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotTitlePackageSizeData

data class SnapshotTitleRenderState(
  val appName: CharSequence,
  val packageName: CharSequence,
  val versionInfo: CharSequence,
  val packageSize: SnapshotTitlePackageSizeRenderState?,
  val apis: CharSequence
)

data class SnapshotTitlePackageSizeRenderState(
  val text: CharSequence,
  val breakStart: Int
)

fun SnapshotTitleDisplayData.toRenderState(): SnapshotTitleRenderState {
  return SnapshotTitleRenderState(
    appName = appName,
    packageName = packageName,
    versionInfo = versionInfo,
    packageSize = packageSize?.toRenderState(),
    apis = apis
  )
}

private fun SnapshotTitlePackageSizeData.toRenderState(): SnapshotTitlePackageSizeRenderState {
  return SnapshotTitlePackageSizeRenderState(
    text = text,
    breakStart = breakStart
  )
}
