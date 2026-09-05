package com.absinthe.libchecker.domain.statistics.chart.usecase

import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.statistics.chart.repository.KotlinVersionRepository
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildDetailedKotlinChartDataUseCaseTest {
  @Test
  fun `groups versions numerically and preserves unknown and unused apps`() = runBlocking {
    val scanned = java.util.Collections.synchronizedList(mutableListOf<String>())
    val useCase = BuildDetailedKotlinChartDataUseCase(object : KotlinVersionRepository {
      override suspend fun getVersion(packageName: String): String? {
        scanned += packageName
        return mapOf("a" to "2.10.0", "b" to "2.9.0", "c" to "2.9.0")[packageName]
      }
    })
    val progress = mutableListOf<Int>()
    val result = useCase(
      listOf(item("a"), item("b"), item("c"), item("unknown"), item("unused", kotlin = false), item("system", system = true)),
      showSystemApps = false,
      onProgress = { progress += it }
    )
    assertEquals(listOf("2.9.0", "2.10.0", null, ""), result.map { it.version })
    assertEquals(listOf("b", "c"), result.first().items.map { it.packageName })
    assertEquals(setOf("a", "b", "c", "unknown"), scanned.toSet())
    assertEquals(5, result.sumOf { it.items.size })
    assertEquals(100, progress.last())
  }

  @Test
  fun `empty input produces no groups and system apps can be included`() = runBlocking {
    val useCase = BuildDetailedKotlinChartDataUseCase(object : KotlinVersionRepository {
      override suspend fun getVersion(packageName: String) = "2.1.x"
    })
    assertEquals(emptyList<KotlinVersionChartGroup>(), useCase(emptyList(), true) {})
    val result = useCase(listOf(item("system", system = true)), true) {}
    assertEquals("2.1.x", result.single().version)
    assertEquals("system", result.single().items.single().packageName)
  }

  @Test
  fun boundsParallelScansAndKeepsInputOrderAndMonotonicProgress() = runBlocking {
    val active = AtomicInteger()
    val maximum = AtomicInteger()
    val bothStarted = CompletableDeferred<Unit>()
    val useCase = BuildDetailedKotlinChartDataUseCase(object : KotlinVersionRepository {
      override suspend fun getVersion(packageName: String): String {
        val count = active.incrementAndGet()
        maximum.updateAndGet { maxOf(it, count) }
        if (count == 2) bothStarted.complete(Unit)
        try {
          bothStarted.await()
          delay(if (packageName == "a") 30 else 1)
          return "2.1.x"
        } finally {
          active.decrementAndGet()
        }
      }
    })
    val progress = mutableListOf<Int>()
    val result = withTimeout(5_000) {
      useCase(listOf(item("a"), item("b"), item("c"), item("d")), true) { progress += it }
    }
    assertEquals(2, maximum.get())
    assertEquals(listOf("a", "b", "c", "d"), result.single().items.map { it.packageName })
    assertEquals(listOf(25, 50, 75, 100), progress)
  }

  private fun item(name: String, kotlin: Boolean = true, system: Boolean = false) = LCItem(
    packageName = name,
    label = name,
    versionName = "1",
    versionCode = 1,
    installedTime = 0,
    lastUpdatedTime = 0,
    isSystem = system,
    abi = 0,
    features = if (kotlin) Features.KOTLIN_USED else 0,
    targetApi = 35,
    variant = 0
  )
}
