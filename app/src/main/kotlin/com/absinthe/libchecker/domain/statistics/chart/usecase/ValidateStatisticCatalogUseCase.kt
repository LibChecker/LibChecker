package com.absinthe.libchecker.domain.statistics.chart.usecase

import com.absinthe.libchecker.domain.statistics.chart.model.STATISTIC_SCHEMA_VERSION
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticBundle
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticCalculationKind
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticComparisonOperator
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticConditionSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDexClassQuery
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDexMethodReference
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticEvidence
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticPredicateValue
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticSource
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticStringOperator
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticTitleSpec
import java.net.URI

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
      definition.details?.let { details ->
        validateTitle(
          titleSpec = details.description,
          statisticId = definition.id,
          errors = errors,
          maxLength = MAX_DESCRIPTION_LENGTH
        )
        if (
          definition.source != StatisticSource.BUILTIN &&
          details.description.translations[DEFAULT_TRANSLATION_LOCALE].isNullOrBlank()
        ) {
          errors += "External statistic details must provide an English description: ${definition.id}"
        }
        if (!isSafeReferenceUrl(details.referenceUrl)) {
          errors += "Statistic details have an invalid reference URL: ${definition.id}"
        }
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
            definition.calculation.predicate != null ||
            definition.calculation.facets != null
          ) {
            errors += "Native statistic has an invalid calculation: ${definition.id}"
          }
          if (definition.source != StatisticSource.BUILTIN) {
            errors += "External statistic cannot invoke a native chart operator: ${definition.id}"
          }
        }

        StatisticCalculationKind.PREDICATE -> {
          val predicate = definition.calculation.predicate
          if (
            predicate == null ||
            definition.calculation.nativeOperator != null ||
            definition.calculation.facets != null
          ) {
            errors += "Predicate statistic has an invalid calculation: ${definition.id}"
          } else {
            validateTitle(predicate.matchedTitle, definition.id, errors)
            validateTitle(predicate.unmatchedTitle, definition.id, errors)
            val legacyPartCount = listOfNotNull(
              predicate.evidence,
              predicate.operator,
              predicate.value
            ).size
            val condition = when {
              predicate.condition != null && legacyPartCount == 0 -> predicate.condition

              predicate.condition == null && legacyPartCount == 3 -> StatisticConditionSpec(
                evidence = predicate.evidence,
                operator = predicate.operator,
                value = predicate.value
              )

              else -> null
            }
            if (condition == null) {
              errors += "Predicate statistic must define exactly one complete condition: ${definition.id}"
            } else {
              validateCondition(condition, definition.id, errors)
            }
          }
        }

        StatisticCalculationKind.FACETS -> {
          val facets = definition.calculation.facets
          if (
            facets == null ||
            definition.calculation.nativeOperator != null ||
            definition.calculation.predicate != null
          ) {
            errors += "Facets statistic has an invalid calculation: ${definition.id}"
          } else {
            validateTitle(facets.matchedTitle, definition.id, errors)
            validateTitle(facets.unmatchedTitle, definition.id, errors)
            if (facets.items.isEmpty() || facets.items.size > MAX_FACETS) {
              errors += "Facets statistic has an invalid facet count: ${definition.id}"
            }
            val duplicateFacetIds = facets.items.groupingBy { it.id }.eachCount()
              .filterValues { it > 1 }
              .keys
            duplicateFacetIds.forEach {
              errors += "Facets statistic has a duplicate facet id: ${definition.id}: $it"
            }
            val conditionState = ConditionValidationState()
            facets.items.forEach { facet ->
              if (!FACET_ID.matches(facet.id)) {
                errors += "Facets statistic has an invalid facet id: ${definition.id}: ${facet.id}"
              }
              validateTitle(
                titleSpec = facet.title,
                statisticId = definition.id,
                errors = errors,
                maxLength = MAX_FACET_TITLE_LENGTH
              )
              if (
                definition.source != StatisticSource.BUILTIN &&
                facet.title.translations[DEFAULT_TRANSLATION_LOCALE].isNullOrBlank()
              ) {
                errors += "External facet must provide an English title: ${definition.id}: ${facet.id}"
              }
              facet.shortTitle?.let { shortTitle ->
                validateTitle(
                  titleSpec = shortTitle,
                  statisticId = definition.id,
                  errors = errors,
                  maxLength = MAX_FACET_TITLE_LENGTH
                )
                if (
                  definition.source != StatisticSource.BUILTIN &&
                  shortTitle.translations[DEFAULT_TRANSLATION_LOCALE].isNullOrBlank()
                ) {
                  errors += "External facet must provide an English short title: ${definition.id}: ${facet.id}"
                }
              }
              validateCondition(
                condition = facet.condition,
                statisticId = definition.id,
                errors = errors,
                state = conditionState
              )
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

  private fun isSafeReferenceUrl(url: String): Boolean {
    if (url.length > MAX_REFERENCE_URL_LENGTH || url.any(Char::isWhitespace)) return false
    return runCatching { URI(url) }.getOrNull()?.let { uri ->
      uri.scheme.equals("https", ignoreCase = true) &&
        !uri.host.isNullOrBlank() &&
        uri.userInfo == null
    } == true
  }

  private fun validateTitle(
    titleSpec: StatisticTitleSpec,
    statisticId: String,
    errors: MutableList<String>,
    maxLength: Int = MAX_TITLE_LENGTH
  ) {
    val hasResourceTitle = titleSpec.resource != null
    val hasTranslatedTitle = titleSpec.translations.isNotEmpty()
    if (hasResourceTitle == hasTranslatedTitle) {
      errors += "Statistic must have exactly one title source: $statisticId"
    }
    if (titleSpec.translations.any { (locale, title) ->
        !LOCALE_TAG.matches(locale) || title.isBlank() || title.length > maxLength
      }
    ) {
      errors += "Statistic has an invalid translated title: $statisticId"
    }
  }

  private fun validateCondition(
    condition: StatisticConditionSpec,
    statisticId: String,
    errors: MutableList<String>,
    depth: Int = 1,
    state: ConditionValidationState = ConditionValidationState()
  ) {
    state.nodes++
    if (depth > MAX_CONDITION_DEPTH) {
      if (!state.reportedDepth) {
        errors += "Predicate condition exceeds maximum depth: $statisticId"
        state.reportedDepth = true
      }
      return
    }
    if (state.nodes > MAX_CONDITION_NODES) {
      if (!state.reportedNodes) {
        errors += "Predicate condition has too many nodes: $statisticId"
        state.reportedNodes = true
      }
      return
    }

    val hasLeaf = condition.evidence != null || condition.operator != null || condition.value != null
    val kindCount = listOf(
      hasLeaf,
      condition.all != null,
      condition.any != null,
      condition.not != null
    ).count { it }
    if (kindCount != 1) {
      errors += "Predicate condition must define exactly one operation: $statisticId"
      return
    }
    if (hasLeaf) {
      val evidence = condition.evidence
      val operator = condition.operator
      val value = condition.value
      if (evidence == null || operator == null || value == null) {
        errors += "Predicate evidence condition is incomplete: $statisticId"
      } else {
        validateEvidence(evidence, operator, value, statisticId, errors)
      }
      return
    }

    val children = condition.all ?: condition.any
    if (children != null) {
      if (children.isEmpty() || children.size > MAX_CONDITION_CHILDREN) {
        errors += "Predicate logical condition has an invalid child count: $statisticId"
        return
      }
      children.forEach { validateCondition(it, statisticId, errors, depth + 1, state) }
      return
    }
    validateCondition(checkNotNull(condition.not), statisticId, errors, depth + 1, state)
  }

  private fun validateEvidence(
    evidence: StatisticEvidence,
    operator: StatisticComparisonOperator,
    value: StatisticPredicateValue,
    statisticId: String,
    errors: MutableList<String>
  ) {
    val valueCount = listOfNotNull(
      value.integer,
      value.boolean,
      value.string,
      value.strings,
      value.dexClasses,
      value.manifestAttribute
    ).size
    if (valueCount != 1) {
      errors += "Predicate evidence must have exactly one value: $statisticId"
    }
    when (evidence) {
      StatisticEvidence.TARGET_SDK -> {
        if (value.integer == null) {
          errors += "Target SDK predicate requires an integer value: $statisticId"
        }
        if (
          operator !in setOf(
            StatisticComparisonOperator.EQUAL,
            StatisticComparisonOperator.GREATER_THAN_OR_EQUAL,
            StatisticComparisonOperator.LESS_THAN_OR_EQUAL
          )
        ) {
          errors += "Target SDK predicate requires a numeric operator: $statisticId"
        }
      }

      StatisticEvidence.NATIVE_LIBRARY -> {
        val libraryName = value.string
        if (libraryName == null || !NATIVE_LIBRARY_NAME.matches(libraryName)) {
          errors += "Native library predicate requires a valid library name: $statisticId"
        }
        if (operator != StatisticComparisonOperator.CONTAINS) {
          errors += "Native library predicate requires the contains operator: $statisticId"
        }
      }

      StatisticEvidence.DEX_CLASS -> {
        val queries = value.dexClasses
        if (queries == null || queries.isEmpty() || queries.size > MAX_DEX_CLASS_QUERIES) {
          errors += "DEX class predicate requires valid class queries: $statisticId"
        } else {
          queries.forEach { validateDexClassQuery(it, statisticId, errors) }
        }
        if (operator != StatisticComparisonOperator.CONTAINS_ANY) {
          errors += "DEX class predicate requires the contains_any operator: $statisticId"
        }
      }

      StatisticEvidence.ARCHIVE_ENTRY -> {
        val entries = value.strings
        if (
          entries == null || entries.isEmpty() || entries.size > MAX_STRING_VALUES ||
          entries.any { !isSafeArchiveEntryName(it) }
        ) {
          errors += "Archive entry predicate requires valid entry names: $statisticId"
        }
        if (operator != StatisticComparisonOperator.CONTAINS_ANY) {
          errors += "Archive entry predicate requires the contains_any operator: $statisticId"
        }
      }

      StatisticEvidence.MANIFEST_RECEIVER_ACTION -> {
        val actions = value.strings
        if (
          actions == null || actions.isEmpty() || actions.size > MAX_STRING_VALUES ||
          actions.any { !MANIFEST_ACTION.matches(it) }
        ) {
          errors += "Manifest receiver action predicate requires valid actions: $statisticId"
        }
        if (operator != StatisticComparisonOperator.CONTAINS_ANY) {
          errors += "Manifest receiver action predicate requires the contains_any operator: $statisticId"
        }
      }

      StatisticEvidence.MANIFEST_ATTRIBUTE -> {
        val attribute = value.manifestAttribute
        if (attribute == null || !MANIFEST_ATTRIBUTE_NAME.matches(attribute.name)) {
          errors += "Manifest attribute predicate requires a valid Boolean attribute: $statisticId"
        }
        if (operator != StatisticComparisonOperator.EQUAL) {
          errors += "Manifest attribute predicate requires the equal operator: $statisticId"
        }
      }
    }
  }

  private fun validateDexClassQuery(
    query: StatisticDexClassQuery,
    statisticId: String,
    errors: MutableList<String>
  ) {
    if (query.name == null && query.stringConstants.isEmpty() && query.methodReferences.isEmpty()) {
      errors += "DEX class query must define at least one constraint: $statisticId"
    }
    query.name?.let { pattern ->
      if (!DEX_CLASS_PATTERN.matches(pattern.value)) {
        errors += "DEX class query has an invalid class pattern: $statisticId"
      }
      if (pattern.operator == StatisticStringOperator.EQUAL && !pattern.value.endsWith(';')) {
        errors += "Exact DEX class query must use a complete descriptor: $statisticId"
      }
    }
    if (
      query.stringConstants.size > MAX_STRING_VALUES ||
      query.stringConstants.any { !isSafeDexString(it) }
    ) {
      errors += "DEX class query has invalid string constants: $statisticId"
    }
    if (query.methodReferences.size > MAX_METHOD_REFERENCES) {
      errors += "DEX class query has too many method references: $statisticId"
    }
    query.methodReferences.forEach { validateMethodReference(it, statisticId, errors) }
  }

  private fun validateMethodReference(
    reference: StatisticDexMethodReference,
    statisticId: String,
    errors: MutableList<String>
  ) {
    if (!DEX_CLASS_DESCRIPTOR.matches(reference.definingClass) || !DEX_METHOD_NAME.matches(reference.name)) {
      errors += "DEX class query has an invalid method reference: $statisticId"
    }
    if (
      reference.parameterTypes?.let { parameters ->
        parameters.size > MAX_METHOD_PARAMETERS || parameters.any { !DEX_PARAMETER_TYPE.matches(it) }
      } == true
    ) {
      errors += "DEX class query has invalid method parameter types: $statisticId"
    }
  }

  private fun isSafeDexString(value: String): Boolean {
    return value.isNotEmpty() && value.length <= MAX_EVIDENCE_STRING_LENGTH &&
      value.all { it >= ' ' && it != '\u007f' }
  }

  private fun isSafeArchiveEntryName(value: String): Boolean {
    return ARCHIVE_ENTRY_NAME.matches(value) &&
      !value.endsWith('/') &&
      value.split('/').none { segment -> segment == "." || segment == ".." }
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
    const val MAX_FACET_TITLE_LENGTH = 40
    const val MAX_DESCRIPTION_LENGTH = 1_500
    const val MAX_REFERENCE_URL_LENGTH = 512
    const val MAX_FACETS = 8
    const val MAX_EVIDENCE_STRING_LENGTH = 160
    const val MAX_CONDITION_DEPTH = 8
    const val MAX_CONDITION_NODES = 64
    const val MAX_CONDITION_CHILDREN = 16
    const val MAX_DEX_CLASS_QUERIES = 16
    const val MAX_STRING_VALUES = 16
    const val MAX_METHOD_REFERENCES = 16
    const val MAX_METHOD_PARAMETERS = 16
    val STATISTIC_ID = Regex("[a-z][a-z0-9]*(?:[.-][a-z0-9]+)+")
    val FACET_ID = Regex("[a-z][a-z0-9]*(?:[.-][a-z0-9]+)*")
    val LOCALE_TAG = Regex("[A-Za-z]{2,3}(?:-[A-Za-z0-9]{2,8})*")
    val NATIVE_LIBRARY_NAME = Regex("[A-Za-z0-9._+-]{1,160}")
    val ARCHIVE_ENTRY_NAME = Regex("[A-Za-z0-9][A-Za-z0-9._+/-]{0,159}")
    val MANIFEST_ACTION = Regex("[A-Za-z0-9_.-]{1,160}")
    val MANIFEST_ATTRIBUTE_NAME = Regex("android:[A-Za-z][A-Za-z0-9_]{0,79}")
    val DEX_CLASS_PATTERN = Regex("L[A-Za-z0-9_$/-]{1,158};?")
    val DEX_CLASS_DESCRIPTOR = Regex("L[A-Za-z0-9_$/-]{1,158};")
    val DEX_METHOD_NAME = Regex("[A-Za-z0-9_$<>-]{1,80}")
    val DEX_PARAMETER_TYPE = Regex("\\[*[ZBSCIJFD]|\\[*L[A-Za-z0-9_$/-]{1,158};")
    const val DEFAULT_TRANSLATION_LOCALE = "en"
  }

  private data class ConditionValidationState(
    var nodes: Int = 0,
    var reportedDepth: Boolean = false,
    var reportedNodes: Boolean = false
  )
}
