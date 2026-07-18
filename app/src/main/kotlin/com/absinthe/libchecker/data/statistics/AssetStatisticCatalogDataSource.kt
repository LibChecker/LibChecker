package com.absinthe.libchecker.data.statistics

import android.content.Context
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticBundle
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDefinition
import com.absinthe.libchecker.domain.statistics.chart.usecase.ValidateStatisticCatalogUseCase
import com.absinthe.libchecker.utils.JsonUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AssetStatisticCatalogDataSource(
  context: Context,
  private val validateCatalog: ValidateStatisticCatalogUseCase
) : BuiltInStatisticCatalogDataSource {
  private val assets = context.assets

  override suspend fun getStatistics(): List<StatisticDefinition> = withContext(Dispatchers.IO) {
    val bundle = assets.open(CATALOG_ASSET).bufferedReader().use { reader ->
      checkNotNull(JsonUtil.moshi.adapter(StatisticBundle::class.java).fromJson(reader.readText())) {
        "Built-in statistic catalog is empty"
      }
    }
    val errors = validateCatalog(bundle)
    check(errors.isEmpty()) {
      "Invalid built-in statistic catalog: ${errors.joinToString()}"
    }
    bundle.definitions
  }

  private companion object {
    const val CATALOG_ASSET = "statistics/v1/catalog.json"
  }
}
