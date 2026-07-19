package com.absinthe.libchecker.data.statistics

import com.absinthe.libchecker.domain.statistics.chart.model.StatisticCalculationKind
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticCalculationSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDefinition
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDrawableIcon
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticIconSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticNativeOperator
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticSource
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticTitleResource
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticTitleSpec
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultStatisticCatalogRepositoryTest {

  @Test
  fun firstLoadSelectsOnlyBuiltInStatistics() = runBlocking {
    val store = FakeSelectionStore()
    val repository = createRepository(store = store)

    val selected = repository.getSelectedStatistics()

    assertEquals(listOf("builtin.abi", "builtin.kotlin"), selected.map(StatisticDefinition::id))
    assertEquals(listOf("builtin.abi", "builtin.kotlin"), store.ids)
  }

  @Test
  fun savedOrderCanMixBuiltInAndOfficialStatistics() = runBlocking {
    val store = FakeSelectionStore(
      listOf("official.target-sdk-35-plus", "builtin.kotlin", "missing.rule")
    )
    val repository = createRepository(store = store)

    val selected = repository.getSelectedStatistics()

    assertEquals(
      listOf("official.target-sdk-35-plus", "builtin.kotlin"),
      selected.map(StatisticDefinition::id)
    )
  }

  @Test
  fun refreshingCatalogDoesNotAppendOnlineStatisticsToSelection() = runBlocking {
    val store = FakeSelectionStore(listOf("builtin.abi"))
    val repository = createRepository(
      store = store,
      refreshedOfficial = listOf(officialDefinition(revision = 2))
    )

    val refreshed = checkNotNull(repository.refreshAvailableStatistics())
    val selected = repository.getSelectedStatistics()

    assertEquals(2, refreshed.last().revision)
    assertEquals(listOf("builtin.abi"), selected.map(StatisticDefinition::id))
  }

  @Test
  fun savingSelectionPreservesOrderAndRemovesDuplicates() = runBlocking {
    val store = FakeSelectionStore()
    val repository = createRepository(store = store)

    repository.setSelectedStatisticIds(
      listOf("builtin.kotlin", "builtin.abi", "builtin.kotlin")
    )

    assertEquals(listOf("builtin.kotlin", "builtin.abi"), store.ids)
  }

  private fun createRepository(
    store: FakeSelectionStore,
    refreshedOfficial: List<StatisticDefinition>? = listOf(officialDefinition())
  ): DefaultStatisticCatalogRepository {
    return DefaultStatisticCatalogRepository(
      builtIn = object : BuiltInStatisticCatalogDataSource {
        override suspend fun getStatistics(): List<StatisticDefinition> = listOf(
          builtInDefinition("builtin.abi", StatisticTitleResource.ABI),
          builtInDefinition("builtin.kotlin", StatisticTitleResource.KOTLIN)
        )
      },
      official = object : RemoteStatisticCatalogDataSource {
        override suspend fun getCachedStatistics(): List<StatisticDefinition> = listOf(officialDefinition())

        override suspend fun refreshStatistics(): List<StatisticDefinition>? = refreshedOfficial
      },
      selectionStore = store
    )
  }

  private fun builtInDefinition(
    id: String,
    titleResource: StatisticTitleResource
  ): StatisticDefinition {
    return StatisticDefinition(
      id = id,
      revision = 1,
      source = StatisticSource.BUILTIN,
      title = StatisticTitleSpec(resource = titleResource),
      icon = StatisticIconSpec(drawable = StatisticDrawableIcon.ABI),
      calculation = StatisticCalculationSpec(
        kind = StatisticCalculationKind.NATIVE,
        nativeOperator = StatisticNativeOperator.ABI
      )
    )
  }

  private fun officialDefinition(revision: Int = 1): StatisticDefinition {
    return StatisticDefinition(
      id = "official.target-sdk-35-plus",
      revision = revision,
      source = StatisticSource.OFFICIAL,
      title = StatisticTitleSpec(translations = mapOf("en" to "Target SDK 35+")),
      icon = StatisticIconSpec(asset = "icons/android-15.svg"),
      calculation = StatisticCalculationSpec(kind = StatisticCalculationKind.PREDICATE)
    )
  }

  private class FakeSelectionStore(
    var ids: List<String>? = null
  ) : StatisticSelectionStore {
    override fun getSelectedStatisticIds(): List<String>? = ids

    override fun setSelectedStatisticIds(ids: List<String>) {
      this.ids = ids
    }
  }
}
