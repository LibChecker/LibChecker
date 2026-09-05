package com.absinthe.libchecker.domain.statistics.chart.usecase

import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticComparisonOperator
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticConditionSpec
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticDexClassQuery
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticEvidence
import com.absinthe.libchecker.domain.statistics.chart.model.StatisticPredicateValue
import com.absinthe.libchecker.domain.statistics.chart.repository.StatisticArtifactQuery
import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticConditionEvaluatorTest {
  private val evaluator = StatisticConditionEvaluator()
  private val item = LCItem("app", "app", "1", 1, 0, 0, false, 0, 0, 35, 0)
  private val sdk = StatisticConditionSpec(
    evidence = StatisticEvidence.TARGET_SDK,
    operator = StatisticComparisonOperator.EQUAL,
    value = StatisticPredicateValue(integer = 35)
  )
  private val zip = StatisticConditionSpec(
    evidence = StatisticEvidence.ARCHIVE_ENTRY,
    operator = StatisticComparisonOperator.CONTAINS_ANY,
    value = StatisticPredicateValue(strings = listOf("marker"))
  )
  private val manifest = StatisticConditionSpec(
    evidence = StatisticEvidence.MANIFEST_RECEIVER_ACTION,
    operator = StatisticComparisonOperator.CONTAINS_ANY,
    value = StatisticPredicateValue(strings = listOf("action"))
  )
  private val dex = StatisticConditionSpec(
    evidence = StatisticEvidence.DEX_CLASS,
    operator = StatisticComparisonOperator.CONTAINS_ANY,
    value = StatisticPredicateValue(dexClasses = listOf(StatisticDexClassQuery()))
  )

  @Test
  fun `staged evaluation equals eager truth tables for nested all any and not`() {
    val leaves = listOf(sdk, zip, manifest, dex)
    val conditions = leaves.flatMap { left ->
      leaves.flatMap { right ->
        listOf(
          StatisticConditionSpec(all = listOf(left, right)),
          StatisticConditionSpec(any = listOf(left, right)),
          StatisticConditionSpec(not = StatisticConditionSpec(any = listOf(left, right))),
          StatisticConditionSpec(all = listOf(StatisticConditionSpec(not = left), StatisticConditionSpec(any = listOf(right, dex))))
        )
      }
    } + listOf(StatisticConditionSpec(all = emptyList()), StatisticConditionSpec(any = emptyList()))
    val queries = conditions.flatMapTo(LinkedHashSet(), evaluator::collectArtifactQueries).toList()
    for (mask in 0 until (1 shl queries.size)) {
      val evidence = queries.mapIndexed { index, query -> query to (mask and (1 shl index) != 0) }.toMap()
      val queried = mutableSetOf<StatisticArtifactQuery>()
      val result = evaluator.evaluateStaged(item, conditions) { requested ->
        check(requested.none { it in queried })
        queried += requested
        requested.associateWith { evidence.getValue(it) }
      }
      assertEquals(conditions.map { evaluator.matches(item, it, evidence) }, result)
    }
  }

  @Test
  fun `field decisive branches perform no artifact lookup regardless of order`() {
    val conditions = listOf(
      sdk,
      StatisticConditionSpec(any = listOf(dex, sdk)),
      StatisticConditionSpec(all = listOf(dex, StatisticConditionSpec(not = sdk))),
      StatisticConditionSpec(not = StatisticConditionSpec(any = listOf(dex, sdk)))
    )
    assertEquals(listOf(true, true, false, false), evaluator.evaluateStaged(item, conditions) { error("Unexpected package query") })
  }

  @Test
  fun `archive hit skips manifest and DEX and shared facet evidence is batched once`() {
    val calls = mutableListOf<Set<StatisticArtifactQuery>>()
    val condition = StatisticConditionSpec(any = listOf(dex, manifest, zip))
    val result = evaluator.evaluateStaged(item, listOf(condition, condition)) { requested ->
      calls += requested
      requested.associateWith { true }
    }
    assertEquals(listOf(true, true), result)
    assertEquals(listOf(setOf(StatisticArtifactQuery.ArchiveEntries(listOf("marker")))), calls)
  }
}
