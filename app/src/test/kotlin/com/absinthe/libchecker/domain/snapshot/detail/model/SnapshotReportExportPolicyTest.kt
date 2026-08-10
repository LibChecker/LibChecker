package com.absinthe.libchecker.domain.snapshot.detail.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotReportExportPolicyTest {

  @Test
  fun keepsShortReportsOnClipboard() {
    assertEquals(
      SnapshotReportExportTarget.CLIPBOARD,
      chooseSnapshotReportExportTarget("a".repeat(MAX_SNAPSHOT_REPORT_CLIPBOARD_BYTES))
    )
  }

  @Test
  fun sharesLongReportsAsFiles() {
    assertEquals(
      SnapshotReportExportTarget.TEXT_FILE,
      chooseSnapshotReportExportTarget("a".repeat(MAX_SNAPSHOT_REPORT_CLIPBOARD_BYTES + 1))
    )
  }

  @Test
  fun measuresUtf8BytesInsteadOfCharacters() {
    assertEquals(
      SnapshotReportExportTarget.TEXT_FILE,
      chooseSnapshotReportExportTarget("更".repeat(MAX_SNAPSHOT_REPORT_CLIPBOARD_BYTES / 3 + 1))
    )
  }
}
