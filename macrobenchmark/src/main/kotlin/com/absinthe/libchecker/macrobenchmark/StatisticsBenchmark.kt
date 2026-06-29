package com.absinthe.libchecker.macrobenchmark

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
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
class StatisticsBenchmark {

  @get:Rule
  val benchmarkRule = MacrobenchmarkRule()

  @Test
  fun coldOpenStatisticsReference() = benchmarkRule.measureRepeated(
    packageName = TARGET_PACKAGE,
    metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
    compilationMode = CompilationMode.None(),
    startupMode = StartupMode.COLD,
    iterations = 5,
    setupBlock = {
      pressHome()
    }
  ) {
    startActivityAndWait(
      Intent(ACTION_STATISTICS).apply {
        setClassName(TARGET_PACKAGE, MAIN_ACTIVITY)
        flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
      }
    )
    check(
      device.wait(
        Until.hasObject(By.pkg(TARGET_PACKAGE).descContains(STATISTICS_READY_TEXT)),
        UI_TIMEOUT_MS
      )
    ) {
      "Statistics page did not show the reference list"
    }
  }

  private companion object {
    private const val TARGET_PACKAGE = "com.absinthe.libchecker.debug"
    private const val MAIN_ACTIVITY = "com.absinthe.libchecker.domain.home.ui.MainActivity"
    private const val ACTION_STATISTICS = "com.absinthe.libchecker.intent.action.START_STATISTICS"
    private const val STATISTICS_READY_TEXT = "Jetpack App Startup"
    private const val UI_TIMEOUT_MS = 45_000L
  }
}
