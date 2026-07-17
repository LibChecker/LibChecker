package com.absinthe.libchecker.domain.snapshot.detail.ui.model

import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailSummary
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotTitleDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotTitlePackageSizeData
import java.text.NumberFormat

data class SnapshotTitleRenderState(
  val appName: CharSequence,
  val iconContentDescription: CharSequence,
  val packageName: CharSequence,
  val versionInfo: CharSequence,
  val packageSize: SnapshotTitlePackageSizeRenderState?,
  val apis: CharSequence,
  val summary: SnapshotDetailSummaryRenderState?,
  val copyPrimaryText: Boolean
)

data class SnapshotDetailSummaryRenderState(
  val totalCountText: String,
  val counts: List<SnapshotDetailCountRenderState>,
  val contentDescription: String
)

data class SnapshotTitlePackageSizeRenderState(
  val text: CharSequence,
  val breakStart: Int
)

fun SnapshotTitleDisplayData.toRenderState(
  summary: SnapshotDetailSummary? = null,
  copyPrimaryText: Boolean = true
): SnapshotTitleRenderState {
  return SnapshotTitleRenderState(
    appName = appName,
    iconContentDescription = appName,
    packageName = packageName,
    versionInfo = versionInfo,
    packageSize = packageSize?.toRenderState(),
    apis = apis,
    summary = summary?.takeIf { it.totalCount > 0 }?.toRenderState(),
    copyPrimaryText = copyPrimaryText
  )
}

private fun SnapshotDetailSummary.toRenderState(): SnapshotDetailSummaryRenderState {
  return SnapshotDetailSummaryRenderState(
    totalCountText = NumberFormat.getIntegerInstance().format(totalCount),
    counts = statusCounts.map {
      SnapshotDetailCountRenderState(
        diffType = it.diffType,
        iconRes = it.status.iconRes,
        countText = it.countText,
        colorRes = it.status.colorRes
      )
    },
    contentDescription = description
  )
}

private fun SnapshotTitlePackageSizeData.toRenderState(): SnapshotTitlePackageSizeRenderState {
  return SnapshotTitlePackageSizeRenderState(
    text = text,
    breakStart = breakStart
  )
}
