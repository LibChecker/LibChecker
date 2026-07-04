package com.absinthe.libchecker.domain.snapshot.detail.model

data class SnapshotTitleDisplayData(
  val appName: CharSequence,
  val packageName: CharSequence,
  val versionInfo: CharSequence,
  val packageSize: SnapshotTitlePackageSizeData?,
  val apis: CharSequence
)

data class SnapshotTitlePackageSizeData(
  val text: CharSequence,
  val breakStart: Int
)

fun buildSnapshotDetailReportHeader(data: SnapshotTitleDisplayData): String {
  return buildString {
    appendLine(data.appName)
    appendLine(data.packageName)
    appendLine(data.versionInfo)
    appendLine(data.apis)
    data.packageSize?.let {
      appendLine(it.text)
    }
  }
}
