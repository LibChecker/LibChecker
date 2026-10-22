package com.absinthe.libchecker.domain.snapshot.timenode.ui

import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.domain.home.ui.MainActivity
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.backup.usecase.BuildSnapshotRestorePlanUseCase
import com.absinthe.libchecker.domain.snapshot.backup.usecase.RestoreSnapshotArchiveFromUriUseCase
import com.absinthe.libchecker.domain.snapshot.backup.usecase.SnapshotRestorePlan
import com.absinthe.libchecker.domain.snapshot.comparison.usecase.CompareSnapshotListsUseCase
import com.absinthe.libchecker.domain.snapshot.timenode.usecase.ComputeSnapshotContributionsUseCase
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class TimeNodePickerCompatibilityInstrumentedTest {
  @Test
  fun importsExplicitFixtureThroughArchiveRestoreAndComparesSnapshots() = runBlocking {
    val path = InstrumentationRegistry.getArguments().getString("snapshotFixturePath")
    assumeTrue("Fixture import is opt-in and leaves the imported snapshots for UI validation", path != null)
    val file = File(requireNotNull(path))
    require(file.canonicalPath.startsWith("/data/local/tmp/"))
    val uri = Uri.fromFile(file)
    val koin = GlobalContext.get()
    assertEquals(SnapshotRestorePlan.ArchiveBackup, koin.get<BuildSnapshotRestorePlanUseCase>()(uri))
    val result = requireNotNull(koin.get<RestoreSnapshotArchiveFromUriUseCase>()(uri))
    assertEquals(3, result.timeStampCounts.size)
    assertEquals(9, result.timeStampCounts.values.sum())
    val repository = koin.get<SnapshotRepository>()
    val timestamps = result.timeStampCounts.keys.sorted()
    timestamps.forEach { assertEquals(3, repository.getSnapshots(it).size) }
    val diffs = koin.get<CompareSnapshotListsUseCase>()(
      repository.getSnapshots(timestamps.first()),
      repository.getSnapshots(timestamps.last()),
      emptySet()
    )
    assertEquals(3, diffs.size)
    assertEquals(1, diffs.count { it.newInstalled })
    assertEquals(1, diffs.count { it.deleted })
    assertEquals(1, diffs.count { !it.newInstalled && !it.deleted })
    val contributions = koin.get<ComputeSnapshotContributionsUseCase>()(
      repository.getTimeStamps().filter { it.timestamp in timestamps }
    )
    assertTrue(contributions.totalUpdates > 0)
  }

  @Test
  fun snapshotAndComparisonPickersOpenWithSelectedDate() {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      listOf(false to false, true to true, true to false).forEach { (compare, left) ->
        lateinit var dialog: TimeNodeBottomSheetDialogFragment
        scenario.onActivity { activity ->
          dialog = TimeNodeBottomSheetDialogFragment.newInstance(
            arrayListOf(),
            System.currentTimeMillis()
          ).apply {
            setCompareMode(compare)
            setLeftMode(left)
          }
          dialog.showNow(activity.supportFragmentManager, "time-node-compatibility")
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        scenario.onActivity {
          assertTrue(dialog.requireDialog().isShowing)
          assertTrue(dialog.requireView().isAttachedToWindow)
          dialog.dismissNow()
        }
      }
    }
  }
}
