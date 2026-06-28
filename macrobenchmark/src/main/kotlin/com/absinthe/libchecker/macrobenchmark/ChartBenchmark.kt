package com.absinthe.libchecker.macrobenchmark

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import java.util.regex.Pattern
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ChartBenchmark {

  @get:Rule
  val benchmarkRule = MacrobenchmarkRule()

  @Test
  @OptIn(ExperimentalMetricApi::class)
  fun show16KBPageSizeChart() = benchmarkRule.measureRepeated(
    packageName = TARGET_PACKAGE,
    metrics = listOf(
      FrameTimingMetric(),
      TraceSectionMetric(TRACE_16KB_GET_PACKAGE_INFO),
      TraceSectionMetric(TRACE_16KB_CHECK_SOURCE),
      TraceSectionMetric(TRACE_16KB_CHECK_BASE_SOURCE),
      TraceSectionMetric(TRACE_16KB_CHECK_SPLIT_SOURCE),
      TraceSectionMetric(TRACE_16KB_PRECHECK_BASE_APK),
      TraceSectionMetric(TRACE_16KB_SCAN_APK),
      TraceSectionMetric(TRACE_16KB_OPEN_ZIP),
      TraceSectionMetric(TRACE_16KB_MATCH_ZIP_ENTRIES),
      TraceSectionMetric(TRACE_16KB_PARSE_ELF),
      TraceSectionMetric(TRACE_16KB_ZIP_ALIGNMENT),
      TraceSectionMetric(TRACE_16KB_NATIVE_DIR)
    ),
    compilationMode = CompilationMode.None(),
    iterations = 5,
    setupBlock = {
      pressHome()
      startActivityAndWait {
        it.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
      }
      waitForAppList()
      openChartActivity()
      waitFor16KBSelector()
    }
  ) {
    val selector = device.wait(
      Until.findObject(By.pkg(TARGET_PACKAGE).desc(CHART_16KB_SELECTOR_TEXT_PATTERN)),
      UI_TIMEOUT_MS
    ) ?: error("Could not find the 16 KB selector")
    selector.click()

    check(
      device.wait(
        Until.hasObject(By.pkg(TARGET_PACKAGE).text(CHART_NOT_SUPPORTED_TEXT_PATTERN)),
        UI_TIMEOUT_MS
      )
    ) {
      "16 KB chart did not show the unsupported bucket"
    }
    check(
      device.wait(
        Until.hasObject(By.pkg(TARGET_PACKAGE).text(CHART_NO_NATIVE_LIBS_TEXT_PATTERN)),
        UI_TIMEOUT_MS
      )
    ) {
      "16 KB chart did not show the no-native-libs bucket"
    }
  }

  private fun MacrobenchmarkScope.openChartActivity() {
    startActivityAndWait(
      Intent().apply {
        setClassName(TARGET_PACKAGE, CHART_ACTIVITY)
        flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
      }
    )
  }

  private fun MacrobenchmarkScope.waitForAppList() {
    check(
      device.wait(
        Until.hasObject(By.res(ANDROID_PACKAGE, APP_LIST_RES_ID)),
        UI_TIMEOUT_MS
      )
    ) {
      "App list did not show a RecyclerView"
    }
    check(
      device.wait(
        Until.hasObject(By.pkg(TARGET_PACKAGE).textStartsWith(PACKAGE_NAME_PREFIX)),
        UI_TIMEOUT_MS
      )
    ) {
      "App list did not show a visible package row"
    }
  }

  private fun MacrobenchmarkScope.waitFor16KBSelector() {
    check(
      device.wait(
        Until.hasObject(By.pkg(TARGET_PACKAGE).desc(CHART_16KB_SELECTOR_TEXT_PATTERN)),
        UI_TIMEOUT_MS
      )
    ) {
      "Chart page did not show the 16 KB selector"
    }
  }

  private companion object {
    private const val TARGET_PACKAGE = "com.absinthe.libchecker.debug"
    private const val ANDROID_PACKAGE = "android"
    private const val CHART_ACTIVITY = "com.absinthe.libchecker.domain.statistics.chart.ui.ChartActivity"
    private const val APP_LIST_RES_ID = "list"
    private const val PACKAGE_NAME_PREFIX = "com."
    private const val UI_TIMEOUT_MS = 60_000L
    private const val TRACE_16KB_GET_PACKAGE_INFO = "LC 16KB getPackageInfo"
    private const val TRACE_16KB_CHECK_SOURCE = "LC 16KB checkSource"
    private const val TRACE_16KB_CHECK_BASE_SOURCE = "LC 16KB checkBaseSource"
    private const val TRACE_16KB_CHECK_SPLIT_SOURCE = "LC 16KB checkSplitSource"
    private const val TRACE_16KB_PRECHECK_BASE_APK = "LC 16KB precheckBaseApk"
    private const val TRACE_16KB_SCAN_APK = "LC 16KB scanApk"
    private const val TRACE_16KB_OPEN_ZIP = "LC 16KB openZip"
    private const val TRACE_16KB_MATCH_ZIP_ENTRIES = "LC 16KB matchZipEntries"
    private const val TRACE_16KB_PARSE_ELF = "LC 16KB parseElf"
    private const val TRACE_16KB_ZIP_ALIGNMENT = "LC 16KB zipAlign"
    private const val TRACE_16KB_NATIVE_DIR = "LC 16KB nativeDir"

    private val CHART_16KB_SELECTOR_TEXT_PATTERN: Pattern = Pattern.compile(
      ".*16\\s*KB.*",
      Pattern.CASE_INSENSITIVE
    )
    private val CHART_NOT_SUPPORTED_TEXT_PATTERN: Pattern = Pattern.compile("Not supported|不支持")
    private val CHART_NO_NATIVE_LIBS_TEXT_PATTERN: Pattern = Pattern.compile(
      "Apps without native libraries|无原生库的应用"
    )
  }
}
