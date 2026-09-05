package com.absinthe.libchecker.domain.snapshot.list.capture

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import com.absinthe.libchecker.domain.snapshot.SnapshotSettingsRepository
import com.absinthe.libchecker.domain.snapshot.selection.SnapshotSelection
import com.absinthe.libchecker.domain.snapshot.selection.SnapshotSelectionRepository
import com.absinthe.libchecker.domain.snapshot.timenode.usecase.RefreshSnapshotRepresentativeAppsUseCase
import com.absinthe.libchecker.domain.snapshot.timenode.usecase.SelectSnapshotRepresentativeAppsUseCase
import com.absinthe.libchecker.utils.extensions.getPackageSize
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@Suppress("DEPRECATION")
class CaptureSnapshotBatchInstrumentedTest {
  @Test
  fun unchangedPackagesReuseCompleteRowsWithOneReadPerBatch() = runBlocking {
    captureAndVerify(fullCapture = false)
  }

  @Test
  fun fullCaptureDoesNotReadPreviousRows() = runBlocking {
    captureAndVerify(fullCapture = true)
  }

  private suspend fun captureAndVerify(fullCapture: Boolean) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val packages = List(101) { index ->
      PackageInfo().apply {
        packageName = "com.example.batch$index"
        versionCode = 1
        lastUpdateTime = 1
        applicationInfo = ApplicationInfo(context.applicationInfo)
      }
    }
    val oldRows = packages.associate { info ->
      info.packageName to SnapshotItem(
        id = 42,
        packageName = info.packageName,
        timeStamp = 1,
        label = "Example",
        versionName = "1",
        versionCode = 1,
        isArchived = false,
        installedTime = 1,
        lastUpdatedTime = 1,
        isSystem = false,
        abi = 0,
        targetApi = 35,
        nativeLibs = "native payload",
        services = "service payload",
        activities = "[]",
        receivers = "[]",
        providers = "[]",
        permissions = "[]",
        metadata = "metadata payload",
        packageSize = info.getPackageSize(true),
        compileSdk = 35,
        minSdk = 24,
        statsVersion = SnapshotItem.CURRENT_STATS_VERSION,
        dexStatsAvailable = true,
        resourceStatsAvailable = true
      )
    }
    val readSizes = mutableListOf<Int>()
    val writeSizes = mutableListOf<Int>()
    val saved = mutableListOf<SnapshotItem>()
    val repository = fake<SnapshotRepository> { method, args ->
      when (method) {
        "getSnapshots" -> {
          assertEquals(1L, args[0])
          @Suppress("UNCHECKED_CAST")
          val names = args[1] as List<String>
          readSizes.add(names.size)
          names.map { oldRows.getValue(it) }
        }

        "insertSnapshots" -> {
          @Suppress("UNCHECKED_CAST")
          val rows = args[0] as List<SnapshotItem>
          if (rows.isNotEmpty()) writeSizes.add(rows.size)
          saved.addAll(rows)
          Unit
        }

        "deleteAllSnapshotDiffItems", "insertTimeStamp" -> Unit

        "getTimeStamp" -> null

        else -> error("Unexpected repository operation: $method")
      }
    }
    val selection = object : SnapshotSelectionRepository {
      override var currentTimestamp = 1L
    }
    val useCase = CaptureInstalledSnapshotUseCase(
      context.packageManager,
      repository,
      fake<InstalledAppRepository> { method, _ ->
        check(fullCapture && method == "getPackageInfo") { "Unexpected package scan: $method" }
        null
      },
      SnapshotSelection(selection),
      fake<SnapshotSettingsRepository> { method, _ ->
        check(method == "getAutoRemoveThreshold")
        0
      },
      object : SnapshotCaptureStateRepository {
        override fun shouldSaveFullSnapshot() = fullCapture
        override fun markFullSnapshotSaved() {
          check(fullCapture)
        }
      },
      RefreshSnapshotRepresentativeAppsUseCase(repository, SelectSnapshotRepresentativeAppsUseCase())
    )

    val result = useCase(CaptureInstalledSnapshotUseCase.Request(packages, false, emptyMap(), 2)) {}

    assertEquals(if (fullCapture) emptyList<Int>() else listOf(50, 50, 1), readSizes)
    assertEquals(if (fullCapture) emptyList<Int>() else listOf(50, 50, 1), writeSizes)
    assertEquals(101, result.processedCount)
    assertEquals(2L, selection.currentTimestamp)
    saved.forEach { row ->
      assertNull(row.id)
      assertEquals(oldRows.getValue(row.packageName).copy(id = null, timeStamp = 2), row)
    }
  }

  private inline fun <reified T> fake(crossinline invoke: (String, Array<out Any?>) -> Any?): T {
    return Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, args ->
      invoke(method.name, args.orEmpty())
    } as T
  }
}
