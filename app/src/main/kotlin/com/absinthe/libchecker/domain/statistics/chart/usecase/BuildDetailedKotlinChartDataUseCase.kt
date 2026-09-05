package com.absinthe.libchecker.domain.statistics.chart.usecase

import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.statistics.chart.repository.KotlinVersionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

class BuildDetailedKotlinChartDataUseCase(
  private val kotlinVersionRepository: KotlinVersionRepository
) {
  suspend operator fun invoke(
    items: List<LCItem>,
    showSystemApps: Boolean,
    onProgress: suspend (Int) -> Unit
  ): List<KotlinVersionChartGroup> = withContext(Dispatchers.IO) {
    val targets = items.filter { showSystemApps || !it.isSystem }
    val scanSlots = Semaphore(2)
    val progressMutex = Mutex()
    var completed = 0
    var lastProgress = -1
    val versions = targets.map { item ->
      async {
        scanSlots.withPermit {
          currentCoroutineContext().ensureActive()
          val version = if (item.features and Features.KOTLIN_USED == 0) {
            UNUSED
          } else {
            kotlinVersionRepository.getVersion(item.packageName)
          }
          progressMutex.withLock {
            completed++
            val progress = completed * 100 / targets.size
            if (progress != lastProgress) {
              lastProgress = progress
              onProgress(progress)
            }
          }
          version
        }
      }
    }.awaitAll()
    val groups = linkedMapOf<String?, MutableList<LCItem>>()
    targets.forEachIndexed { index, item ->
      groups.getOrPut(versions[index]) { mutableListOf() }.add(item)
    }
    groups.entries.sortedWith { a, b ->
      compareValuesBy(a.key, b.key, { it == UNUSED }, { it == null }).takeIf { it != 0 }
        ?: compareVersions(a.key.orEmpty(), b.key.orEmpty())
    }.map { (version, apps) -> KotlinVersionChartGroup(version, apps) }
  }

  private fun compareVersions(left: String, right: String): Int {
    val a = left.split('.', '-')
    val b = right.split('.', '-')
    for (index in 0 until maxOf(a.size, b.size)) {
      val x = a.getOrNull(index).orEmpty()
      val y = b.getOrNull(index).orEmpty()
      val comparison = if (x.toIntOrNull() != null && y.toIntOrNull() != null) {
        x.toInt().compareTo(y.toInt())
      } else {
        x.compareTo(y)
      }
      if (comparison != 0) return comparison
    }
    return 0
  }

  companion object {
    const val UNUSED = ""
  }
}

data class KotlinVersionChartGroup(val version: String?, val items: List<LCItem>)
