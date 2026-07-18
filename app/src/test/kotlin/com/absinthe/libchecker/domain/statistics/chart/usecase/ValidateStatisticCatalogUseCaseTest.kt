package com.absinthe.libchecker.domain.statistics.chart.usecase

import com.absinthe.libchecker.domain.statistics.chart.model.StatisticBundle
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticCalculationKind
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticCalculationSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticComparisonOperator
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticConditionSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDefinition
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDexClassQuery
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDexMethodReference
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDrawableIcon
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticEvidence
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticIconSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticNativeOperator
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticPredicateSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticPredicateValue
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticSource
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticStringOperator
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticStringPattern
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticTitleResource
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticTitleSpec
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateStatisticCatalogUseCaseTest {

  private val validate = ValidateStatisticCatalogUseCase()

  @Test
  fun `accepts a built-in rule backed by a drawable`() {
    val errors = validate(StatisticBundle(1, listOf(builtInDefinition())))

    assertTrue(errors.toString(), errors.isEmpty())
  }

  @Test
  fun `accepts an official rule backed by a translated title and SVG`() {
    val definition = officialDefinition()

    val errors = validate(StatisticBundle(1, listOf(definition)))

    assertTrue(errors.toString(), errors.isEmpty())
  }

  @Test
  fun `accepts a native library predicate`() {
    val definition = officialDefinition().copy(
      id = "official.flutter",
      calculation = StatisticCalculationSpec(
        kind = StatisticCalculationKind.PREDICATE,
        predicate = StatisticPredicateSpec(
          evidence = StatisticEvidence.NATIVE_LIBRARY,
          operator = StatisticComparisonOperator.CONTAINS,
          value = StatisticPredicateValue(string = "libflutter.so"),
          matchedTitle = StatisticTitleSpec(translations = mapOf("en" to "Flutter apps")),
          unmatchedTitle = StatisticTitleSpec(translations = mapOf("en" to "Other apps"))
        )
      )
    )

    val errors = validate(StatisticBundle(1, listOf(definition)))

    assertTrue(errors.toString(), errors.isEmpty())
  }

  @Test
  fun `rejects incompatible native library predicate`() {
    val definition = officialDefinition().copy(
      id = "official.flutter",
      calculation = StatisticCalculationSpec(
        kind = StatisticCalculationKind.PREDICATE,
        predicate = StatisticPredicateSpec(
          evidence = StatisticEvidence.NATIVE_LIBRARY,
          operator = StatisticComparisonOperator.EQUAL,
          value = StatisticPredicateValue(string = "../libflutter.so"),
          matchedTitle = StatisticTitleSpec(translations = mapOf("en" to "Flutter apps")),
          unmatchedTitle = StatisticTitleSpec(translations = mapOf("en" to "Other apps"))
        )
      )
    )

    val errors = validate(StatisticBundle(1, listOf(definition)))

    assertTrue(errors.any { it.startsWith("Native library predicate requires a valid library name") })
    assertTrue(errors.any { it.startsWith("Native library predicate requires the contains operator") })
  }

  @Test
  fun `accepts composite DEX and manifest evidence without app-specific keys`() {
    val definition = officialDefinition().copy(
      id = "official.artifact-evidence",
      calculation = StatisticCalculationSpec(
        kind = StatisticCalculationKind.PREDICATE,
        predicate = StatisticPredicateSpec(
          condition = StatisticConditionSpec(
            any = listOf(
              StatisticConditionSpec(
                evidence = StatisticEvidence.DEX_CLASS,
                operator = StatisticComparisonOperator.CONTAINS_ANY,
                value = StatisticPredicateValue(
                  dexClasses = listOf(
                    StatisticDexClassQuery(
                      name = StatisticStringPattern(
                        operator = StatisticStringOperator.STARTS_WITH,
                        value = "Lcom/example/"
                      ),
                      stringConstants = listOf("com.example.ACTION"),
                      methodReferences = listOf(
                        StatisticDexMethodReference(
                          definingClass = "Landroid/content/IntentFilter;",
                          name = "addAction"
                        )
                      )
                    )
                  )
                )
              ),
              StatisticConditionSpec(
                evidence = StatisticEvidence.MANIFEST_RECEIVER_ACTION,
                operator = StatisticComparisonOperator.CONTAINS_ANY,
                value = StatisticPredicateValue(strings = listOf("com.example.ACTION"))
              )
            )
          ),
          matchedTitle = StatisticTitleSpec(translations = mapOf("en" to "Matched")),
          unmatchedTitle = StatisticTitleSpec(translations = mapOf("en" to "Other"))
        )
      )
    )

    val errors = validate(StatisticBundle(1, listOf(definition)))

    assertTrue(errors.toString(), errors.isEmpty())
  }

  @Test
  fun `rejects unconstrained DEX class evidence`() {
    val definition = officialDefinition().copy(
      id = "official.unconstrained-dex",
      calculation = StatisticCalculationSpec(
        kind = StatisticCalculationKind.PREDICATE,
        predicate = StatisticPredicateSpec(
          condition = StatisticConditionSpec(
            evidence = StatisticEvidence.DEX_CLASS,
            operator = StatisticComparisonOperator.CONTAINS_ANY,
            value = StatisticPredicateValue(dexClasses = listOf(StatisticDexClassQuery()))
          ),
          matchedTitle = StatisticTitleSpec(translations = mapOf("en" to "Matched")),
          unmatchedTitle = StatisticTitleSpec(translations = mapOf("en" to "Other"))
        )
      )
    )

    val errors = validate(StatisticBundle(1, listOf(definition)))

    assertTrue(errors.any { it.startsWith("DEX class query must define at least one constraint") })
  }

  @Test
  fun `rejects duplicate ids and external rules using built-in drawables`() {
    val definition = builtInDefinition().copy(
      source = StatisticSource.OFFICIAL
    )

    val errors = validate(StatisticBundle(1, listOf(definition, definition)))

    assertTrue(errors.any { it.startsWith("Duplicate statistic id") })
    assertTrue(errors.any { it.startsWith("Statistic source and id do not match") })
    assertTrue(errors.any { it.startsWith("External statistic must use an SVG icon") })
  }

  @Test
  fun `rejects traversal in SVG path`() {
    val definition = builtInDefinition().copy(
      id = "official.unsafe",
      source = StatisticSource.OFFICIAL,
      title = StatisticTitleSpec(translations = mapOf("en" to "Unsafe")),
      icon = StatisticIconSpec(asset = "icons/../unsafe.svg")
    )

    val errors = validate(StatisticBundle(1, listOf(definition)))

    assertTrue(errors.any { it.startsWith("Statistic must have exactly one valid icon source") })
  }

  private fun builtInDefinition() = StatisticDefinition(
    id = "builtin.abi",
    revision = 1,
    source = StatisticSource.BUILTIN,
    title = StatisticTitleSpec(resource = StatisticTitleResource.ABI),
    icon = StatisticIconSpec(drawable = StatisticDrawableIcon.ABI),
    calculation = StatisticCalculationSpec(
      kind = StatisticCalculationKind.NATIVE,
      nativeOperator = StatisticNativeOperator.ABI
    )
  )

  private fun officialDefinition() = StatisticDefinition(
    id = "official.target-sdk-35-plus",
    revision = 1,
    source = StatisticSource.OFFICIAL,
    title = StatisticTitleSpec(translations = mapOf("en" to "Target SDK 35+")),
    icon = StatisticIconSpec(asset = "icons/android-15.svg"),
    calculation = StatisticCalculationSpec(
      kind = StatisticCalculationKind.PREDICATE,
      predicate = StatisticPredicateSpec(
        evidence = StatisticEvidence.TARGET_SDK,
        operator = StatisticComparisonOperator.GREATER_THAN_OR_EQUAL,
        value = StatisticPredicateValue(integer = 35),
        matchedTitle = StatisticTitleSpec(translations = mapOf("en" to "Target SDK 35 or newer")),
        unmatchedTitle = StatisticTitleSpec(translations = mapOf("en" to "Target SDK 34 or older"))
      )
    )
  )
}
