package com.absinthe.libchecker.domain.statistics.chart.usecase

import com.absinthe.libchecker.domain.statistics.chart.model.STATISTIC_SCHEMA_VERSION
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticBundle
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticCalculationKind
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticComparisonOperator
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticEvidence
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticSource
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticTitleSpec

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
      validateTitle(definition.title, definition.id, errors)
      if (definition.source == StatisticSource.BUILTIN && definition.title.resource == null) {
        errors += "Built-in statistic must use a resource title: ${definition.id}"
      } else if (definition.source != StatisticSource.BUILTIN && definition.title.translations.isEmpty()) {
        errors += "External statistic must use translated titles: ${definition.id}"
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
          if (
            definition.calculation.nativeOperator == null ||
            definition.calculation.predicate != null
          ) {
            errors += "Native statistic has an invalid calculation: ${definition.id}"
          }
          if (definition.source != StatisticSource.BUILTIN) {
            errors += "External statistic cannot invoke a native chart operator: ${definition.id}"
          }
        }

        StatisticCalculationKind.PREDICATE -> {
          val predicate = definition.calculation.predicate
          if (predicate == null || definition.calculation.nativeOperator != null) {
            errors += "Predicate statistic has an invalid calculation: ${definition.id}"
          } else {
            validateTitle(predicate.matchedTitle, definition.id, errors)
            validateTitle(predicate.unmatchedTitle, definition.id, errors)
            val valueCount = listOfNotNull(
              predicate.value.integer,
              predicate.value.boolean,
              predicate.value.string
            ).size
            if (valueCount != 1) {
              errors += "Predicate statistic must have exactly one value: ${definition.id}"
            }
            when (predicate.evidence) {
              StatisticEvidence.TARGET_SDK -> {
                if (predicate.value.integer == null) {
                  errors += "Target SDK predicate requires an integer value: ${definition.id}"
                }
                if (predicate.operator == StatisticComparisonOperator.CONTAINS) {
                  errors += "Target SDK predicate requires a numeric operator: ${definition.id}"
                }
              }

              StatisticEvidence.NATIVE_LIBRARY -> {
                val libraryName = predicate.value.string
                if (libraryName == null || !NATIVE_LIBRARY_NAME.matches(libraryName)) {
                  errors += "Native library predicate requires a valid library name: ${definition.id}"
                }
                if (predicate.operator != StatisticComparisonOperator.CONTAINS) {
                  errors += "Native library predicate requires the contains operator: ${definition.id}"
                }
              }
            }
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

  private fun validateTitle(
    titleSpec: StatisticTitleSpec,
    statisticId: String,
    errors: MutableList<String>
  ) {
    val hasResourceTitle = titleSpec.resource != null
    val hasTranslatedTitle = titleSpec.translations.isNotEmpty()
    if (hasResourceTitle == hasTranslatedTitle) {
      errors += "Statistic must have exactly one title source: $statisticId"
    }
    if (titleSpec.translations.any { (locale, title) ->
        !LOCALE_TAG.matches(locale) || title.isBlank() || title.length > MAX_TITLE_LENGTH
      }
    ) {
      errors += "Statistic has an invalid translated title: $statisticId"
    }
  }

  private val StatisticSource.idPrefix: String
    get() = when (this) {
      StatisticSource.BUILTIN -> "builtin"
      StatisticSource.OFFICIAL -> "official"
      StatisticSource.CUSTOM -> "custom"
    }

  private companion object {
    const val ICON_ASSET_PREFIX = "icons/"
    const val MAX_ASSET_PATH_LENGTH = 160
    const val MAX_TITLE_LENGTH = 80
    val STATISTIC_ID = Regex("[a-z][a-z0-9]*(?:[.-][a-z0-9]+)+")
    val LOCALE_TAG = Regex("[A-Za-z]{2,3}(?:-[A-Za-z0-9]{2,8})*")
    val NATIVE_LIBRARY_NAME = Regex("[A-Za-z0-9._+-]{1,160}")
  }
}
