package com.absinthe.libchecker.domain.snapshot.comparison.presentation

import com.absinthe.libchecker.domain.snapshot.comparison.model.SnapshotComparisonInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ComparisonDashboardStatePlannerTest {

  private val labels = ComparisonDashboardStatePlanner.Labels(
    timestampTitle = "Current snapshot timestamp",
    chooseTimestampText = "Click to choose",
    appsCountTitle = "Count of apps in Snapshot",
    defaultAppsCountText = "0"
  )

  @Test
  fun plansUnselectedSideDescriptionFromDefaultDisplayText() {
    val state = ComparisonDashboardStatePlanner.planSideState(
      input = SnapshotComparisonInput(),
      labels = labels,
      formatTimestamp = { "unused" }
    )

    assertEquals("Click to choose", state.timestampText)
    assertEquals("0", state.appsCountText)
    assertEquals(
      "Current snapshot timestamp, Click to choose, Count of apps in Snapshot, 0",
      state.contentDescription
    )
    assertNull(state.dashboardCountTimestamp)
  }

  @Test
  fun plansSnapshotSideDescriptionFromTimestampText() {
    val state = ComparisonDashboardStatePlanner.planSideState(
      input = SnapshotComparisonInput.snapshot(1680000000000L),
      labels = labels,
      formatTimestamp = { "2026-06-11, 20:26:22" }
    )

    assertEquals("2026-06-11, 20:26:22", state.timestampText)
    assertEquals("0", state.appsCountText)
    assertEquals(
      "Current snapshot timestamp, 2026-06-11, 20:26:22, Count of apps in Snapshot, 0",
      state.contentDescription
    )
    assertEquals(1680000000000L, state.dashboardCountTimestamp)
  }

  @Test
  fun updatesSnapshotAppsCountDescription() {
    val state = ComparisonDashboardStatePlanner.planSideState(
      input = SnapshotComparisonInput.snapshot(1680000000000L),
      labels = labels,
      formatTimestamp = { "2026-06-11, 20:26:22" }
    )

    val updatedState = state.withAppsCountText("330", labels)

    assertEquals("330", updatedState.appsCountText)
    assertEquals(
      "Current snapshot timestamp, 2026-06-11, 20:26:22, Count of apps in Snapshot, 330",
      updatedState.contentDescription
    )
  }
}
