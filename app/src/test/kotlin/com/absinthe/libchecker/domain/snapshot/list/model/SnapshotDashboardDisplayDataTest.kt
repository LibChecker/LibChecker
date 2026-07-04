package com.absinthe.libchecker.domain.snapshot.list.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotDashboardDisplayDataTest {

  @Test
  fun displayDataCarriesDashboardDescriptions() {
    val displayData = SnapshotDashboardDisplayData(
      timestampText = "2026-06-11, 20:26:22",
      appsCountText = "330 / 331",
      systemProps = emptyList(),
      contentDescription = "Current snapshot timestamp, 2026-06-11, 20:26:22, Apps count, 330 / 331",
      timestampContentDescription = "Change timestamp, 2026-06-11, 20:26:22"
    )

    assertEquals("2026-06-11, 20:26:22", displayData.timestampText)
    assertEquals("330 / 331", displayData.appsCountText)
    assertTrue(displayData.systemProps.isEmpty())
    assertEquals(
      "Current snapshot timestamp, 2026-06-11, 20:26:22, Apps count, 330 / 331",
      displayData.contentDescription
    )
    assertEquals(
      "Change timestamp, 2026-06-11, 20:26:22",
      displayData.timestampContentDescription
    )
  }

  @Test
  fun buildsDashboardDescriptionFromVisibleTextParts() {
    assertEquals(
      "Current snapshot timestamp, 2026-06-11, 20:26:22, Apps count, 330 / 331",
      buildSnapshotDashboardDescription(
        timestampTitle = "Current snapshot timestamp",
        timestampText = "2026-06-11, 20:26:22",
        appsCountTitle = "Apps count",
        appsCountText = "330 / 331"
      )
    )
  }

  @Test
  fun buildsTimestampActionDescriptionFromVisibleTimestamp() {
    assertEquals(
      "Change timestamp, 2026-06-11, 20:26:22",
      buildSnapshotDashboardTimestampDescription(
        actionTitle = "Change timestamp",
        timestampText = "2026-06-11, 20:26:22"
      )
    )
  }

  @Test
  fun skipsBlankDashboardDescriptionParts() {
    assertEquals(
      "Current snapshot timestamp, Apps count",
      buildSnapshotDashboardDescription(
        timestampTitle = "Current snapshot timestamp",
        timestampText = " ",
        appsCountTitle = "Apps count",
        appsCountText = ""
      )
    )
  }
}
