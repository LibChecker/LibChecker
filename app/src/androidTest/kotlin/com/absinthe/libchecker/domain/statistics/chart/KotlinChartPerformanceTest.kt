package com.absinthe.libchecker.domain.statistics.chart

import android.os.Bundle
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.data.statistics.AndroidKotlinVersionRepository
import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.domain.app.repository.AppListRepository
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.domain.statistics.chart.repository.KotlinVersionRepository
import com.absinthe.libchecker.domain.statistics.chart.usecase.BuildDetailedKotlinChartDataUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class KotlinChartPerformanceTest {
  @Test
  fun measureInstalledAppCohort() = runBlocking {
    val koin = GlobalContext.get()
    val items = koin.get<AppListRepository>().getItems().filter {
      !it.isSystem && !it.packageName.startsWith("com.absinthe.libchecker")
    }.sortedBy { it.packageName }
    assertTrue("Requires initialized real app list", items.size >= 10)
    report("COHORT " + items.joinToString(";") { "${it.packageName}:${it.versionCode}:${it.lastUpdatedTime}:${it.features}" })
    report("COUNTS apps=${items.size} kotlin=${items.count { it.features and Features.KOTLIN_USED != 0 }}")
    var expected: List<Pair<String?, List<String>>>? = null
    repeat(3) { round ->
      val variants = if (round % 2 == 0) listOf("before", "serial", "after") else listOf("after", "serial", "before")
      for (variant in variants) {
        val repository: KotlinVersionRepository = if (variant == "before") {
          BaselineKotlinVersionRepository(koin.get<InstalledAppRepository>())
        } else {
          AndroidKotlinVersionRepository(koin.get<InstalledAppRepository>())
        }
        val times = java.util.concurrent.ConcurrentHashMap<String, Long>()
        val measured = object : KotlinVersionRepository {
          override suspend fun getVersion(packageName: String): String? {
            val start = SystemClock.elapsedRealtimeNanos()
            return repository.getVersion(packageName).also {
              times[packageName] = SystemClock.elapsedRealtimeNanos() - start
            }
          }
        }
        val useCase = BuildDetailedKotlinChartDataUseCase(measured)
        for (mode in listOf("cold", "warm")) {
          val start = SystemClock.elapsedRealtimeNanos()
          val groups = if (variant == "after") {
            useCase(items, false) {}
          } else {
            BaselineBuildDetailedKotlinChartDataUseCase(measured)(items, false) {}
          }
          val elapsed = (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000.0
          val result = groups.map { it.version to it.items.map { app -> app.packageName } }
          if (expected == null) expected = result else assertEquals(expected, result)
          report("RESULT variant=$variant round=$round mode=$mode ms=$elapsed")
          report("APPS variant=$variant round=$round mode=$mode " + times.entries.joinToString(";") { "${it.key}=${it.value / 1_000_000.0}" })
        }
      }
    }
    report("GROUPS $expected")
  }

  private fun report(message: String) {
    InstrumentationRegistry.getInstrumentation().sendStatus(
      0,
      Bundle().apply {
        putString("stream", "KOTLIN_PERF $message\n")
      }
    )
  }
}
