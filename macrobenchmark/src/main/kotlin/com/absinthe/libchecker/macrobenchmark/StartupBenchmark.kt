package com.absinthe.libchecker.macrobenchmark

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
class StartupBenchmark {

  @get:Rule
  val benchmarkRule = MacrobenchmarkRule()

  @Test
  fun coldStartupToHome() = benchmarkRule.measureRepeated(
    packageName = TARGET_PACKAGE,
    metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
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
  }
}
