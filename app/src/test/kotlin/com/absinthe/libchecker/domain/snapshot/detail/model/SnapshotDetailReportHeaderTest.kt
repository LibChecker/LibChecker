package com.absinthe.libchecker.domain.snapshot.detail.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotDetailReportHeaderTest {

  @Test
  fun buildsHeaderWithPackageSize() {
    assertEquals(
      """
      LibChecker
      com.absinthe.libchecker
      2.5.4 (2662) -> 2.5.5 (2728)
      Target: 37  Min: 24
      17.52 MB -> 17.34 MB -180 kB

      """.trimIndent(),
      buildSnapshotDetailReportHeader(
        SnapshotTitleDisplayData(
          appName = "LibChecker",
          packageName = "com.absinthe.libchecker",
          versionInfo = "2.5.4 (2662) -> 2.5.5 (2728)",
          packageSize = SnapshotTitlePackageSizeData(
            text = "17.52 MB -> 17.34 MB -180 kB",
            breakStart = 21
          ),
          apis = "Target: 37  Min: 24"
        )
      )
    )
  }

  @Test
  fun skipsMissingPackageSize() {
    assertEquals(
      """
      LibChecker
      com.absinthe.libchecker
      2.5.5 (2728)
      Target: 37

      """.trimIndent(),
      buildSnapshotDetailReportHeader(
        SnapshotTitleDisplayData(
          appName = "LibChecker",
          packageName = "com.absinthe.libchecker",
          versionInfo = "2.5.5 (2728)",
          packageSize = null,
          apis = "Target: 37"
        )
      )
    )
  }
}
