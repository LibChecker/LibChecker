package com.absinthe.libchecker.domain.snapshot.comparison.usecase

import android.content.pm.PackageInfo
import android.database.sqlite.SQLiteFullException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.domain.snapshot.SnapshotItemFactory
import com.absinthe.libchecker.domain.snapshot.SnapshotRepository
import java.lang.reflect.Proxy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompareSnapshotCacheFailureInstrumentedTest {
  @Test
  fun cacheReadFailureStillReturnsComputedDifferences() = verifyCacheFailure("getSnapshotDiffs")

  @Test
  fun cacheWriteFailureStillReturnsComputedDifferences() = verifyCacheFailure("insertSnapshotDiffs")

  private fun verifyCacheFailure(operation: String) = runBlocking {
    val compare = comparison(operation, SQLiteFullException("cache unavailable"))
    val result = compare(1L) {}
    assertEquals("com.example.cache", result?.single()?.packageName)
    assertEquals(1L, result?.single()?.versionCodeDiff?.old)
    assertEquals(2L, result?.single()?.versionCodeDiff?.new)
  }

  @Test
  fun cacheCancellationPropagates() {
    for (operation in listOf("getSnapshotDiffs", "insertSnapshotDiffs")) {
      val canceled = CancellationException("comparison canceled")
      val compare = comparison(operation, canceled)
      assertSame(canceled, assertThrows(CancellationException::class.java) { runBlocking { compare(1L) {} } })
    }
  }

  private fun comparison(failingOperation: String, failure: RuntimeException): CompareSnapshotWithInstalledAppsUseCase {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val previous = SnapshotItem(
      id = null,
      packageName = "com.example.cache",
      timeStamp = 1L,
      label = "Cache test",
      versionName = "1",
      versionCode = 1L,
      isArchived = false,
      installedTime = 1L,
      lastUpdatedTime = 1L,
      isSystem = false,
      abi = 0,
      targetApi = 35,
      nativeLibs = "[]",
      services = "[]",
      activities = "[]",
      receivers = "[]",
      providers = "[]",
      permissions = "[]",
      metadata = "[]",
      packageSize = 1L,
      compileSdk = 35,
      minSdk = 24
    )
    val current = previous.copy(versionCode = 2L, lastUpdatedTime = 2L)
    val packageInfo = PackageInfo().apply {
      packageName = previous.packageName
      longVersionCode = 2L
      lastUpdateTime = 2L
    }
    var failureTriggered = false
    val repository = fake<SnapshotRepository> { method ->
      if (method == failingOperation) {
        failureTriggered = true
        throw failure
      }
      when (method) {
        "getSnapshots" -> listOf(previous)

        "getTrackItems", "getSnapshotDiffs" -> emptyList<Any>()

        "insertSnapshotDiffs" -> {
          check(failureTriggered)
          Unit
        }

        else -> error("Unexpected repository operation: $method")
      }
    }
    return CompareSnapshotWithInstalledAppsUseCase(
      context.packageManager,
      repository,
      fake<InstalledAppRepository> { method ->
        check(method == "getApplicationMap")
        mapOf(packageInfo.packageName to packageInfo)
      },
      object : SnapshotItemFactory {
        override fun create(packageManager: android.content.pm.PackageManager, packageInfo: PackageInfo) = current
      },
      CompareSnapshotItemsUseCase()
    )
  }

  private inline fun <reified T> fake(crossinline invoke: (String) -> Any?): T {
    return Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
      invoke(method.name)
    } as T
  }
}
