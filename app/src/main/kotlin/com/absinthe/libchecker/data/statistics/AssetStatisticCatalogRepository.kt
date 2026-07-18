package com.absinthe.libchecker.data.statistics

import android.content.Context
import com.absinthe.libchecker.domain.statistics.chart.model.STATISTIC_FALLBACK_ICON_ASSET
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticBundle
import com.absinthe.libchecker.domain.statistics.chart.repository.StatisticCatalogRepository
import com.absinthe.libchecker.domain.statistics.chart.usecase.ValidateStatisticCatalogUseCase
import com.absinthe.libchecker.domain.statistics.chart.usecase.ValidateStatisticSvgUseCase
import com.absinthe.libchecker.utils.JsonUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class AssetStatisticCatalogRepository(
  context: Context,
  private val validateCatalog: ValidateStatisticCatalogUseCase,
  private val validateSvg: ValidateStatisticSvgUseCase
) : StatisticCatalogRepository {
  private val assets = context.assets

  override suspend fun getStatistics() = withContext(Dispatchers.IO) {
    val bundle = assets.open(CATALOG_ASSET).bufferedReader().use { reader ->
      checkNotNull(JsonUtil.moshi.adapter(StatisticBundle::class.java).fromJson(reader.readText())) {
        "Built-in statistic catalog is empty"
      }
    }
    val errors = validateCatalog(bundle)
    check(errors.isEmpty()) {
      "Invalid built-in statistic catalog: ${errors.joinToString()}"
    }

    bundle.definitions.map { definition ->
      val iconAsset = definition.icon.asset ?: return@map definition
      val iconErrors = runCatching {
        assets.open(iconAsset).use { validateSvg(it.readBytes()) }
      }.getOrElse { error ->
        listOf(error.message ?: "Unable to read SVG")
      }
      if (iconErrors.isEmpty()) {
        definition
      } else {
        Timber.w("Invalid statistic SVG for %s: %s", definition.id, iconErrors.joinToString())
        definition.copy(icon = definition.icon.copy(asset = STATISTIC_FALLBACK_ICON_ASSET))
      }
    }
  }

  private companion object {
    const val CATALOG_ASSET = "statistics/v1/catalog.json"
  }
}
