package com.absinthe.libchecker.domain.statistics.chart.usecase

import com.absinthe.libchecker.domain.statistics.chart.model.StatisticBundle
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticCalculationKind
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticCalculationSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDefinition
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDrawableIcon
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticIconSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticNativeOperator
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticSource
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
    val definition = builtInDefinition().copy(
      id = "official.dex-field-usage",
      source = StatisticSource.OFFICIAL,
      title = StatisticTitleSpec(translations = mapOf("en" to "DEX field usage")),
      icon = StatisticIconSpec(asset = "statistics/official/dex-field-usage.svg")
    )

    val errors = validate(StatisticBundle(1, listOf(definition)))

    assertTrue(errors.toString(), errors.isEmpty())
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
      icon = StatisticIconSpec(asset = "statistics/../unsafe.svg")
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
}
