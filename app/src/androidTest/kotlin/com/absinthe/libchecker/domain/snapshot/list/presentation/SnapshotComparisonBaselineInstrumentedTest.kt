package com.absinthe.libchecker.domain.snapshot.list.presentation

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.database.entity.SnapshotDiffStoringItem
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.domain.app.repository.PackageListLoadException
import com.absinthe.libchecker.domain.snapshot.SnapshotItemFactory
import com.absinthe.libchecker.domain.snapshot.SnapshotListDisplayOptions
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.SnapshotSettingsRepository
import com.absinthe.libchecker.domain.snapshot.comparison.usecase.CompareSnapshotDiffsUseCase
import com.absinthe.libchecker.domain.snapshot.comparison.usecase.CompareSnapshotItemsUseCase
import com.absinthe.libchecker.domain.snapshot.comparison.usecase.CompareSnapshotListsUseCase
import com.absinthe.libchecker.domain.snapshot.comparison.usecase.CompareSnapshotWithInstalledAppsUseCase
import com.absinthe.libchecker.domain.snapshot.display.SnapshotDashboardCounter
import com.absinthe.libchecker.domain.snapshot.list.usecase.BuildSnapshotListUpdatePlanUseCase
import com.absinthe.libchecker.domain.snapshot.list.usecase.DeleteSnapshotTimeStampUseCase
import com.absinthe.libchecker.domain.snapshot.list.usecase.GetSnapshotPackageIconSourcesUseCase
import com.absinthe.libchecker.domain.snapshot.selection.SnapshotSelection
import com.absinthe.libchecker.domain.snapshot.selection.SnapshotSelectionRepository
import com.absinthe.libchecker.domain.snapshot.track.usecase.CompareTrackedSnapshotListsUseCase
import java.lang.reflect.Proxy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class SnapshotComparisonBaselineInstrumentedTest {
  @Test
  fun failedBaselineSwitchKeepsDisplayedBaselineAndRowsTogether() = runBlocking {
    val fixture = Fixture()
    val viewModel = fixture.viewModel()
    val store = ViewModelStore().apply { put("snapshot", viewModel) }
    val displayedTimestamp = AtomicLong()
    val effects = launch(Dispatchers.Main.immediate) {
      viewModel.effect.collect {
        if (it is SnapshotViewModel.Effect.TimeStampChange) displayedTimestamp.set(it.timestamp)
      }
    }
    try {
      withContext(Dispatchers.Main) { viewModel.refreshSnapshotTimestamp(1L) }
      awaitComparison(viewModel)
      assertEquals(1L, viewModel.buildSnapshotListUpdatePlan(emptyList(), false).items.single().versionCodeDiff.old)

      fixture.failScan = true
      withContext(Dispatchers.Main) { viewModel.refreshSnapshotTimestamp(2L, shouldClearDiff = true) }
      awaitComparison(viewModel)
      assertEquals(1L, viewModel.currentTimeStamp)
      assertEquals(1L, displayedTimestamp.get())
      assertEquals(1L, viewModel.selectedSnapshotTimestamp)
      assertEquals(1L, viewModel.buildSnapshotListUpdatePlan(emptyList(), false).items.single().versionCodeDiff.old)

      fixture.failScan = false
      withContext(Dispatchers.Main) { viewModel.refreshSnapshotTimestamp(2L) }
      awaitComparison(viewModel)
      assertEquals(2L, viewModel.currentTimeStamp)
      assertEquals(2L, displayedTimestamp.get())
      assertEquals(2L, viewModel.selectedSnapshotTimestamp)
      assertEquals(2L, viewModel.buildSnapshotListUpdatePlan(emptyList(), false).items.single().versionCodeDiff.old)
    } finally {
      effects.cancelAndJoin()
      withContext(Dispatchers.Main) { store.clear() }
    }
  }

  @Test
  fun failedSwitchAfterAnotherViewModelDeletesBaselineKeepsTheNewRetryTarget() = runBlocking {
    val fixture = Fixture()
    val viewModel = fixture.viewModel()
    val albumViewModel = fixture.viewModel()
    val store = ViewModelStore().apply {
      put("snapshot", viewModel)
      put("album", albumViewModel)
    }
    try {
      withContext(Dispatchers.Main) { viewModel.refreshSnapshotTimestamp(1L) }
      awaitComparison(viewModel)
      albumViewModel.deleteSnapshotTimeStamp(1L)
      assertEquals(1L, viewModel.currentTimeStamp)
      assertEquals(2L, viewModel.selectedSnapshotTimestamp)

      fixture.failScan = true
      withContext(Dispatchers.Main) { viewModel.refreshSelectedSnapshot() }
      awaitComparison(viewModel)
      assertEquals(0L, viewModel.currentTimeStamp)
      assertEquals(2L, viewModel.selectedSnapshotTimestamp)
      assertEquals(emptyList<Any>(), viewModel.buildSnapshotListUpdatePlan(emptyList(), false).items)

      fixture.failScan = false
      withContext(Dispatchers.Main) { viewModel.refreshSelectedSnapshot() }
      awaitComparison(viewModel)
      assertEquals(2L, viewModel.currentTimeStamp)
      assertEquals(2L, viewModel.buildSnapshotListUpdatePlan(emptyList(), false).items.single().versionCodeDiff.old)
    } finally {
      withContext(Dispatchers.Main) { store.clear() }
    }
  }

  @Test
  fun ordinaryRetryAfterFailedSwitchDoesNotReusePreviousBaselineCache() = runBlocking {
    val fixture = Fixture()
    assertEquals(1L, fixture.compare(1L) {}?.single()?.versionCodeDiff?.old)
    fixture.failScan = true
    assertThrows(PackageListLoadException::class.java) {
      runBlocking { fixture.compare(2L, shouldClearDiff = true) {} }
    }
    fixture.failScan = false
    assertEquals(2L, fixture.compare(2L) {}?.single()?.versionCodeDiff?.old)
    assertEquals(1L, fixture.compare(1L) {}?.single()?.versionCodeDiff?.old)
  }

  @Test
  fun canceledBaselineWriteCannotOverwriteTheFollowingComparisonCache() = runBlocking {
    val fixture = Fixture()
    fixture.compare(1L) {}
    val writing = CompletableDeferred<Unit>()
    val releaseWrite = CountDownLatch(1)
    fixture.beforeCacheWrite = {
      fixture.beforeCacheWrite = null
      writing.complete(Unit)
      check(releaseWrite.await(5, TimeUnit.SECONDS))
    }
    val canceled = async(Dispatchers.IO) { fixture.compare(2L, shouldClearDiff = true) {} }
    try {
      withTimeout(5_000) { writing.await() }
      canceled.cancel()
      val retry = async(Dispatchers.IO) { fixture.compare(1L) {} }
      assertNull(withTimeoutOrNull(100) { retry.await() })
      releaseWrite.countDown()
      canceled.cancelAndJoin()
      assertEquals(1L, retry.await()?.single()?.versionCodeDiff?.old)
      assertEquals(1L, fixture.compare(1L) {}?.single()?.versionCodeDiff?.old)
    } finally {
      releaseWrite.countDown()
      canceled.cancelAndJoin()
    }
  }

  private suspend fun awaitComparison(viewModel: SnapshotViewModel) = withTimeout(5_000) {
    while (viewModel.isComparingActive()) delay(10)
  }

  private class Fixture {
    @Volatile var failScan = false

    @Volatile var beforeCacheWrite: (() -> Unit)? = null
    private var timeStamps = listOf(1L, 2L)
    private val selection = SnapshotSelection(object : SnapshotSelectionRepository {
      override var currentTimestamp = 1L
    })
    private val previous = SnapshotItem(
      id = null, packageName = "com.example.baseline", timeStamp = 1L, label = "Baseline",
      versionName = "1", versionCode = 1L, isArchived = false, installedTime = 1L,
      lastUpdatedTime = 1L, isSystem = false, abi = 0, targetApi = 35,
      nativeLibs = "[]", services = "[]", activities = "[]", receivers = "[]", providers = "[]",
      permissions = "[]", metadata = "[]", packageSize = 1L, compileSdk = 35, minSdk = 24
    )
    private val current = previous.copy(versionCode = 3L, lastUpdatedTime = 3L)
    private val packageInfo = PackageInfo().apply {
      packageName = current.packageName
      longVersionCode = current.versionCode
      lastUpdateTime = current.lastUpdatedTime
    }
    private var cache = emptyList<SnapshotDiffStoringItem>()
    private val repository = fake<SnapshotRepository> { method, args ->
      when (method) {
        "getCurrentSnapshotCount" -> emptyFlow<Int>()

        "getSnapshots" -> if (args[0] as Long in timeStamps) {
          listOf(previous.copy(timeStamp = args[0] as Long, versionCode = args[0] as Long))
        } else {
          emptyList<SnapshotItem>()
        }

        "getTimeStamps" -> timeStamps.map { TimeStampItem(it, null, null) }

        "deleteSnapshotsAndTimeStamp" -> {
          timeStamps = timeStamps.filterNot { it == args[0] }
          Unit
        }

        "getTrackItems" -> emptyList<Any>()

        "getSnapshotDiffs" -> cache

        "deleteAllSnapshotDiffItems" -> {
          cache = emptyList()
          Unit
        }

        "insertSnapshotDiffs" -> {
          beforeCacheWrite?.invoke()
          @Suppress("UNCHECKED_CAST")
          val items = args[0] as List<SnapshotDiffStoringItem>
          cache = items
          Unit
        }

        else -> error("Unexpected snapshot operation: $method")
      }
    }
    private val installed = fake<InstalledAppRepository> { method, _ ->
      when (method) {
        "getApplicationMap" -> {
          if (failScan) throw PackageListLoadException(IllegalStateException("scan failed"))
          mapOf(packageInfo.packageName to packageInfo)
        }

        "getApplicationCount" -> 1

        "getApexPackageNames" -> emptySet<String>()

        else -> error("Unexpected installed-app operation: $method")
      }
    }
    private val settings = fake<SnapshotSettingsRepository> { method, _ ->
      when (method) {
        "getOptions" -> 0
        "getListDisplayOptions" -> SnapshotListDisplayOptions()
        else -> error("Unexpected settings operation: $method")
      }
    }
    private val compareItems = CompareSnapshotItemsUseCase()
    val compare = CompareSnapshotDiffsUseCase(
      repository,
      CompareTrackedSnapshotListsUseCase(repository, CompareSnapshotListsUseCase(compareItems)),
      CompareSnapshotWithInstalledAppsUseCase(
        InstrumentationRegistry.getInstrumentation().targetContext.packageManager,
        repository,
        installed,
        object : SnapshotItemFactory {
          override fun create(packageManager: PackageManager, packageInfo: PackageInfo) = current
        },
        compareItems
      )
    )

    fun viewModel(): SnapshotViewModel {
      val koin = GlobalContext.get()
      val iconSources = GetSnapshotPackageIconSourcesUseCase(installed)
      return SnapshotViewModel(
        SnapshotListWorkflow(
          repository = repository,
          appListRepository = koin.get(),
          compareSnapshotDiffs = compare,
          compareSnapshotItemWithInstalledApp = koin.get(),
          compareSnapshotItems = compareItems,
          snapshotDashboardCounter = SnapshotDashboardCounter(repository, installed),
          snapshotDetailSectionBuilder = koin.get(),
          snapshotRepository = repository,
          buildSnapshotCapturePlanUseCase = koin.get(),
          getSnapshotPackageIconSourcesUseCase = iconSources,
          buildSnapshotListUpdatePlanUseCase = BuildSnapshotListUpdatePlanUseCase(iconSources, installed, settings),
          buildSnapshotSystemPropDisplayDataUseCase = koin.get(),
          buildSnapshotTimeNodeListDataUseCase = koin.get(),
          deleteSnapshotTimeStampUseCase = DeleteSnapshotTimeStampUseCase(repository, selection),
          formatSnapshotTimestampUseCase = koin.get(),
          snapshotSelection = selection,
          snapshotSettingsRepository = settings,
          updateSnapshotAutoRemoveThresholdUseCase = koin.get(),
          snapshotTrackChangeRepository = koin.get(),
          computeSnapshotContributionsUseCase = koin.get()
        )
      )
    }
  }

  companion object {
    private inline fun <reified T> fake(crossinline invoke: (String, Array<out Any?>) -> Any?): T {
      return Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, args ->
        invoke(method.name, args.orEmpty())
      } as T
    }
  }
}
