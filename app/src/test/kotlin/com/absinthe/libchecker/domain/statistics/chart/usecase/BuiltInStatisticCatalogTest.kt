package com.absinthe.libchecker.domain.statistics.chart.usecase

import com.absinthe.libchecker.domain.statistics.chart.model.StatisticBundle
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticSource
import com.absinthe.libchecker.utils.JsonUtil
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltInStatisticCatalogTest {

  @Test
  fun `built-in asset parses and passes catalog validation`() {
    val asset = listOf(
      File("src/main/assets/statistics/v1/catalog.json"),
      File("app/src/main/assets/statistics/v1/catalog.json")
    ).first { it.isFile }
    val bundle = JsonUtil.fromJson<StatisticBundle>(asset.readText())

    assertNotNull(bundle)
    bundle ?: return
    assertEquals(9, bundle.definitions.size)
    assertTrue(bundle.definitions.all { it.source == StatisticSource.BUILTIN })
    assertTrue(bundle.definitions.all { it.icon.drawable != null && it.icon.asset == null })
    assertTrue(ValidateStatisticCatalogUseCase()(bundle).isEmpty())
  }
}
