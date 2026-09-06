package com.absinthe.libchecker.database

import android.database.sqlite.SQLiteBlobTooBigException
import androidx.room3.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.data.snapshot.LocalSnapshotRepository
import com.absinthe.libchecker.database.entity.SnapshotSummaryItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.snapshot.selection.SnapshotSelectionRepository
import java.lang.reflect.Proxy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SnapshotRepositoryInstrumentedTest {
  private val selection = object : SnapshotSelectionRepository {
    override var currentTimestamp = 12L
  }
  private val summary = SnapshotSummaryItem(
    id = 1, packageName = "com.example", timeStamp = 12, label = "Example",
    versionName = "1", versionCode = 1, isArchived = false, installedTime = 1,
    lastUpdatedTime = 1, isSystem = false, abi = 0, targetApi = 35, packageSize = 123,
    compileSdk = 35, minSdk = 24, dexInfo = "[1]", resourceInfo = "[2]",
    resourcesSize = 45, statsVersion = 2, dexStatsAvailable = true, resourceStatsAvailable = true
  )

  @Test
  fun oversizedRowsKeepSummaryFallbackAndEmptySelectionSkipsDao() = runBlocking {
    var reads = 0
    val dao = Proxy.newProxyInstance(LCDao::class.java.classLoader, arrayOf(LCDao::class.java)) { _, method, args ->
      when (method.name) {
        "getSnapshotsCountFlow" -> flowOf(1)

        "getSnapshots", "getSnapshot" -> {
          reads++
          throw SQLiteBlobTooBigException()
        }

        "getSnapshotSummaries" -> listOf(summary)

        "getSnapshotSummary" -> if (args!![1] == summary.packageName) summary else null

        else -> error("Unexpected DAO call: ${method.name}")
      }
    } as LCDao
    val repository = LocalSnapshotRepository(dao, selection)
    assertTrue(repository.getSnapshots(12, emptyList()).isEmpty())
    assertEquals(0, reads)
    assertEquals(listOf(summary.toSnapshotItem()), repository.getSnapshots(12))
    assertEquals(summary.toSnapshotItem(), repository.getSnapshot(12, summary.packageName))
    assertEquals(
      listOf(summary.toSnapshotItem()),
      repository.getSnapshots(12, listOf(summary.packageName, "missing"))
    )
  }

  @Test
  fun deleteAndRetainUseTimestampBoundariesWithoutReadingLargeRows() = runBlocking {
    val database = Room.inMemoryDatabaseBuilder(
      InstrumentationRegistry.getInstrumentation().targetContext,
      LCDatabase::class.java
    ).build()
    try {
      val dao = database.lcDao()
      val repository = LocalSnapshotRepository(dao, selection)
      for (timestamp in listOf(12L, 123L, 1234L)) {
        dao.insert(TimeStampItem(timestamp, null, null))
        dao.insert(summary.toSnapshotItem().copy(id = null, timeStamp = timestamp, nativeLibs = "x".repeat(3 * 1024 * 1024)))
      }
      assertEquals(1, repository.currentSnapshotCount.first())
      repository.deleteSnapshotsAndTimeStamp(12)
      assertEquals(0, repository.currentSnapshotCount.first())
      assertNull(repository.getTimeStamp(12))
      assertEquals(mapOf(123L to 1, 1234L to 1), repository.getSnapshotCountsByTimestamp())
      repository.deleteSnapshotsAndTimeStamp(12)
      repository.retainLatestSnapshots(1)
      assertEquals(listOf(1234L), repository.getTimeStamps().map { it.timestamp })
      assertEquals(mapOf(1234L to 1), repository.getSnapshotCountsByTimestamp())
      repository.retainLatestSnapshots(0)
      assertTrue(repository.getTimeStamps().isEmpty())
      assertTrue(repository.getSnapshotCountsByTimestamp().isEmpty())
    } finally {
      database.close()
    }
  }
}
