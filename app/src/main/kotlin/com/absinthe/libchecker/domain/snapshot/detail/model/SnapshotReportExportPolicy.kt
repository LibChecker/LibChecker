package com.absinthe.libchecker.domain.snapshot.detail.model

const val MAX_SNAPSHOT_REPORT_CLIPBOARD_BYTES = 8 * 1024

enum class SnapshotReportExportTarget {
  CLIPBOARD,
  TEXT_FILE
}

fun chooseSnapshotReportExportTarget(report: String): SnapshotReportExportTarget {
  if (report.length > MAX_SNAPSHOT_REPORT_CLIPBOARD_BYTES) {
    return SnapshotReportExportTarget.TEXT_FILE
  }
  return if (report.toByteArray(Charsets.UTF_8).size <= MAX_SNAPSHOT_REPORT_CLIPBOARD_BYTES) {
    SnapshotReportExportTarget.CLIPBOARD
  } else {
    SnapshotReportExportTarget.TEXT_FILE
  }
}
