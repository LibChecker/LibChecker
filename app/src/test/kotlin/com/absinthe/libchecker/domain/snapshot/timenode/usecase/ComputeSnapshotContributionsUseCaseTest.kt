package com.absinthe.libchecker.domain.snapshot.timenode.usecase

import com.absinthe.libchecker.database.entity.SnapshotDiffStoringItem
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.SnapshotUpdatedApp
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.database.entity.TrackItem
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotPalette
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComputeSnapshotContributionsUseCaseTest {

  private val testZoneId = ZoneId.of("UTC")

  private class FakeSnapshotRepository(
    private val updatesByTimestamp: Map<Long, List<Long>> = emptyMap()
  ) : SnapshotRepository {
    val queriedTimestamps = mutableListOf<Long>()
    override val currentSnapshotCount: Flow<Int> = emptyFlow()
    override suspend fun getTimeStamps(): List<TimeStampItem> = emptyList()
    override suspend fun getTimeStamp(timestamp: Long): TimeStampItem? = null
    override suspend fun getSnapshotCountsByTimestamp(): Map<Long, Int> = emptyMap()
    override suspend fun getSnapshotLastUpdatedTimes(timestamp: Long): List<Long> {
      return updatesByTimestamp[timestamp].orEmpty()
    }
    override suspend fun getSnapshotUpdatedApps(timestamp: Long): List<SnapshotUpdatedApp> {
      queriedTimestamps.add(timestamp)
      return updatesByTimestamp[timestamp].orEmpty().mapIndexed { idx, time ->
        SnapshotUpdatedApp("pkg$idx", "App $idx", time)
      }
    }
    override suspend fun getSnapshots(timestamp: Long): List<SnapshotItem> = emptyList()
    override suspend fun getSnapshots(timestamp: Long, packageNames: List<String>): List<SnapshotItem> = emptyList()
    override suspend fun getSnapshotSummaries(timestamp: Long): List<SnapshotItem> = emptyList()
    override suspend fun getSnapshot(timestamp: Long, packageName: String): SnapshotItem? = null
    override suspend fun getSnapshotDiff(packageName: String): SnapshotDiffStoringItem? = null
    override suspend fun getSnapshotDiffs(): List<SnapshotDiffStoringItem> = emptyList()
    override suspend fun getTrackItems(): List<TrackItem> = emptyList()
    override suspend fun insertSnapshots(items: List<SnapshotItem>) {}
    override suspend fun insertTimeStamp(item: TimeStampItem) {}
    override suspend fun insertSnapshotDiff(item: SnapshotDiffStoringItem) {}
    override suspend fun insertSnapshotDiffs(items: List<SnapshotDiffStoringItem>) {}
    override suspend fun insertTrackItem(item: TrackItem) {}
    override suspend fun updateTimeStamp(item: TimeStampItem) {}
    override suspend fun deleteSnapshotsAndTimeStamp(timestamp: Long) {}
    override suspend fun deleteTrackItem(item: TrackItem) {}
    override suspend fun retainLatestSnapshots(count: Int) {}
    override suspend fun deleteDuplicateSnapshotItems() {}
    override suspend fun deleteSnapshotDiff(packageName: String) {}
    override suspend fun deleteAllSnapshotDiffItems() {}
  }

  @Test
  fun `computes contributions across snapshots with correct colors and counts`() = runBlocking {
    val firstDay = LocalDate.now(testZoneId).minusDays(15)
    val nextDay = firstDay.plusDays(4)
    val middleDay = firstDay.plusDays(2)
    val t1 = firstDay.atTime(12, 0).atZone(testZoneId).toInstant().toEpochMilli()
    val t2 = nextDay.atTime(12, 0).atZone(testZoneId).toInstant().toEpochMilli()

    val u1 = middleDay.atTime(10, 0).atZone(testZoneId).toInstant().toEpochMilli()
    val u2 = middleDay.atTime(14, 0).atZone(testZoneId).toInstant().toEpochMilli()
    val u3 = nextDay.atTime(11, 0).atZone(testZoneId).toInstant().toEpochMilli()

    val repository = FakeSnapshotRepository(
      updatesByTimestamp = mapOf(
        t1 to emptyList(),
        t2 to listOf(u1, u2, u3)
      )
    )

    val useCase = ComputeSnapshotContributionsUseCase(repository)
    val timeStamps = listOf(
      TimeStampItem(t2, null, null),
      TimeStampItem(t1, null, null)
    )

    val result = useCase(timeStamps, testZoneId)

    // Palette check
    val expectedColor1 = SnapshotPalette.getColor(0)
    val expectedColor2 = SnapshotPalette.getColor(1)
    assertEquals(expectedColor1, result.snapshotColors[t1])
    assertEquals(expectedColor2, result.snapshotColors[t2])

    val first = result.days[firstDay]
    assertNotNull(first)
    assertTrue(first!!.isSnapshotDay)
    assertEquals(t1, first.snapshotTimestamp)

    val middle = result.days[middleDay]
    assertNotNull(middle)
    assertEquals(2, middle!!.updateCount)
    assertEquals(t1, middle.snapshotTimestamp)
    assertEquals(expectedColor1, middle.snapshotColor)
    assertEquals(t1, middle.previousSnapshotTimestamp)
    assertEquals(t2, middle.currentSnapshotTimestamp)
    assertEquals(2, middle.updatedApps.size)
    assertEquals(t1, middle.updatedApps[0].previousSnapshotTimestamp)
    assertEquals(t2, middle.updatedApps[0].currentSnapshotTimestamp)
    assertEquals(t1, middle.updatedApps[1].previousSnapshotTimestamp)
    assertEquals(t2, middle.updatedApps[1].currentSnapshotTimestamp)

    val next = result.days[nextDay]
    assertNotNull(next)
    assertTrue(next!!.isSnapshotDay)
    assertEquals(1, next.updateCount)
    assertEquals(t2, next.snapshotTimestamp)
    assertEquals(expectedColor2, next.snapshotColor)
    assertEquals(1, next.updatedApps.size)
    assertEquals(t1, next.updatedApps[0].previousSnapshotTimestamp)
    assertEquals(t2, next.updatedApps[0].currentSnapshotTimestamp)
  }

  @Test
  fun `does not query snapshots whose updates cannot be in the displayed range`() = runBlocking {
    val today = LocalDate.now(testZoneId)
    val old = today.minusYears(2).atStartOfDay(testZoneId).toInstant().toEpochMilli()
    val recent = today.minusDays(2).atStartOfDay(testZoneId).toInstant().toEpochMilli()
    val repository = FakeSnapshotRepository()
    val result = ComputeSnapshotContributionsUseCase(repository)(
      listOf(TimeStampItem(old, null, null), TimeStampItem(recent, null, null)),
      testZoneId
    )
    assertTrue(result.days.isNotEmpty())
    assertEquals(listOf(recent), repository.queriedTimestamps)
  }

  @Test(timeout = 5000)
  fun `extreme imported timestamps only populate the visible days`() = runBlocking {
    val repository = FakeSnapshotRepository()
    val result = ComputeSnapshotContributionsUseCase(repository)(
      listOf(TimeStampItem(Long.MIN_VALUE, null, null), TimeStampItem(Long.MAX_VALUE, null, null)),
      testZoneId
    )
    assertTrue(result.days.size in 120..126)
    assertEquals(result.startDate, result.days.keys.minOrNull())
    assertEquals(result.endDate, result.days.keys.maxOrNull())
    assertTrue(result.days.values.all { it.snapshotTimestamp == Long.MIN_VALUE })
    assertEquals(listOf(Long.MAX_VALUE), repository.queriedTimestamps)
  }

  @Test
  fun `first snapshot day keeps an explicit missing baseline`() = runBlocking {
    val day = LocalDate.now(testZoneId).minusDays(1)
    val snapshot = day.atTime(12, 0).atZone(testZoneId).toInstant().toEpochMilli()
    val update = day.atTime(10, 0).atZone(testZoneId).toInstant().toEpochMilli()
    val result = ComputeSnapshotContributionsUseCase(FakeSnapshotRepository(mapOf(snapshot to listOf(update))))(
      listOf(TimeStampItem(snapshot, null, null)),
      testZoneId
    )
    val app = result.days.getValue(day).updatedApps.single()
    assertEquals(null, app.previousSnapshotTimestamp)
    assertEquals(snapshot, app.currentSnapshotTimestamp)
  }
}
