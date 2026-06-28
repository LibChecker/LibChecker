package com.absinthe.libchecker.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

  @get:Rule
  val benchmarkRule = MacrobenchmarkRule()

  @Test
  @OptIn(ExperimentalMetricApi::class)
  fun coldStartupToHome() = benchmarkRule.measureRepeated(
    packageName = TARGET_PACKAGE,
    metrics = listOf(
      StartupTimingMetric(),
      FrameTimingMetric(),
      TraceSectionMetric(TRACE_APP_LIST_GET_CONTENT),
      TraceSectionMetric(TRACE_APP_LIST_GET_ITEMS),
      TraceSectionMetric(TRACE_APP_LIST_FILTER_ITEMS),
      TraceSectionMetric(TRACE_APP_LIST_INITIAL_ITEM_VIEW_STATES),
      TraceSectionMetric(TRACE_APP_LIST_ITEM_VIEW_STATES),
      TraceSectionMetric(TRACE_APP_LIST_PACKAGE_STATES),
      TraceSectionMetric(TRACE_APP_LIST_GET_APPLICATION_MAP),
      TraceSectionMetric(TRACE_APP_LIST_RESOLVE_PACKAGE_STATES),
      TraceSectionMetric(TRACE_APP_LIST_CREATE_ITEM_VIEW_STATES),
      TraceSectionMetric(TRACE_APP_LIST_BUILD_UPDATE_PLAN),
      TraceSectionMetric(TRACE_APP_LIST_APPLY_UPDATE_MAIN),
      TraceSectionMetric(TRACE_APP_LIST_REMAINING_ITEM_VIEW_STATES)
    ),
    compilationMode = CompilationMode.None(),
    startupMode = StartupMode.COLD,
    iterations = 5,
    setupBlock = {
      pressHome()
    }
  ) {
    startActivityAndWait()
    check(
      device.wait(
        Until.hasObject(By.pkg(TARGET_PACKAGE).textStartsWith(PACKAGE_NAME_PREFIX)),
        UI_TIMEOUT_MS
      )
    ) {
      "App list did not show a visible package row"
    }
  }

  private companion object {
    private const val TARGET_PACKAGE = "com.absinthe.libchecker.debug"
    private const val PACKAGE_NAME_PREFIX = "com."
    private const val UI_TIMEOUT_MS = 15_000L
    private const val TRACE_APP_LIST_APPLY_UPDATE_MAIN = "LC AppList applyUpdateMain"
    private const val TRACE_APP_LIST_BUILD_UPDATE_PLAN = "LC AppList buildUpdatePlan"
    private const val TRACE_APP_LIST_CREATE_ITEM_VIEW_STATES = "LC AppList createItemViewStates"
    private const val TRACE_APP_LIST_FILTER_ITEMS = "LC AppList filterItems"
    private const val TRACE_APP_LIST_GET_APPLICATION_MAP = "LC AppList getApplicationMap"
    private const val TRACE_APP_LIST_GET_CONTENT = "LC AppList getContent"
    private const val TRACE_APP_LIST_GET_ITEMS = "LC AppList getItems"
    private const val TRACE_APP_LIST_INITIAL_ITEM_VIEW_STATES = "LC AppList initialItemViewStates"
    private const val TRACE_APP_LIST_ITEM_VIEW_STATES = "LC AppList itemViewStates"
    private const val TRACE_APP_LIST_PACKAGE_STATES = "LC AppList packageStates"
    private const val TRACE_APP_LIST_REMAINING_ITEM_VIEW_STATES = "LC AppList remainingItemViewStates"
    private const val TRACE_APP_LIST_RESOLVE_PACKAGE_STATES = "LC AppList resolvePackageStates"
  }
}
