package com.absinthe.libchecker.domain.statistics.chart.usecase

import com.absinthe.libchecker.domain.statistics.chart.model.STATISTIC_SCHEMA_VERSION
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticBundle
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticCalculationKind
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticSource

class ValidateStatisticCatalogUseCase {

  operator fun invoke(bundle: StatisticBundle): List<String> {
    val errors = mutableListOf<String>()
    if (bundle.schemaVersion != STATISTIC_SCHEMA_VERSION) {
      errors += "Unsupported schema version: ${bundle.schemaVersion}"
    }

    val duplicateIds = bundle.definitions.groupingBy { it.id }.eachCount()
      .filterValues { it > 1 }
      .keys
    duplicateIds.forEach { errors += "Duplicate statistic id: $it" }

    bundle.definitions.forEach { definition ->
      if (!STATISTIC_ID.matches(definition.id)) {
        errors += "Invalid statistic id: ${definition.id}"
      }
      if (!definition.id.startsWith("${definition.source.idPrefix}.")) {
        errors += "Statistic source and id do not match: ${definition.id}"
      }
      if (definition.revision < 1) {
        errors += "Statistic revision must be positive: ${definition.id}"
      }
      val hasResourceTitle = definition.title.resource != null
      val hasTranslatedTitle = definition.title.translations.isNotEmpty()
      if (hasResourceTitle == hasTranslatedTitle) {
        errors += "Statistic must have exactly one title source: ${definition.id}"
      }
      if (definition.title.translations.any { (locale, title) ->
          !LOCALE_TAG.matches(locale) || title.isBlank() || title.length > MAX_TITLE_LENGTH
        }
      ) {
        errors += "Statistic has an invalid translated title: ${definition.id}"
      }
      val hasDrawable = definition.icon.drawable != null
      val hasSvg = definition.icon.asset?.let(::isSafeIconAsset) == true
      if (hasDrawable == hasSvg) {
        errors += "Statistic must have exactly one valid icon source: ${definition.id}"
      } else if (definition.source == StatisticSource.BUILTIN && !hasDrawable) {
        errors += "Built-in statistic must use a drawable icon: ${definition.id}"
      } else if (definition.source != StatisticSource.BUILTIN && !hasSvg) {
        errors += "External statistic must use an SVG icon: ${definition.id}"
      }
      when (definition.calculation.kind) {
        StatisticCalculationKind.NATIVE -> {
          if (definition.calculation.nativeOperator == null) {
            errors += "Native statistic is missing an operator: ${definition.id}"
          }
        }
      }
    }
    return errors
  }

  fun isSafeIconAsset(asset: String): Boolean {
    return asset.startsWith(ICON_ASSET_PREFIX) &&
      asset.endsWith(".svg", ignoreCase = true) &&
      ".." !in asset &&
      '\\' !in asset &&
      asset.length <= MAX_ASSET_PATH_LENGTH
  }

  private val StatisticSource.idPrefix: String
    get() = when (this) {
      StatisticSource.BUILTIN -> "builtin"
      StatisticSource.OFFICIAL -> "official"
      StatisticSource.CUSTOM -> "custom"
    }

  private companion object {
    const val ICON_ASSET_PREFIX = "statistics/"
    const val MAX_ASSET_PATH_LENGTH = 160
    const val MAX_TITLE_LENGTH = 80
    val STATISTIC_ID = Regex("[a-z][a-z0-9]*(?:[.-][a-z0-9]+)+")
    val LOCALE_TAG = Regex("[A-Za-z]{2,3}(?:-[A-Za-z0-9]{2,8})*")
  }
}
