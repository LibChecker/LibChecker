package com.absinthe.libchecker.macrobenchmark

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
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
class AppDetailBenchmark {

  @get:Rule
  val benchmarkRule = MacrobenchmarkRule()

  @Test
  fun openAndroidAutoDetail() = benchmarkRule.measureRepeated(
    packageName = TARGET_PACKAGE,
    metrics = listOf(FrameTimingMetric()),
    compilationMode = CompilationMode.None(),
    iterations = 5,
    setupBlock = {
      pressHome()
      startActivityAndWait {
        it.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
      }
      check(
        device.wait(
          Until.hasObject(By.pkg(TARGET_PACKAGE).text(ANDROID_AUTO_LABEL)),
          UI_TIMEOUT_MS
        )
      ) {
        "App list did not show $ANDROID_AUTO_LABEL"
      }
    }
  ) {
    val item = device.wait(
      Until.findObject(By.pkg(TARGET_PACKAGE).text(ANDROID_AUTO_LABEL)),
      UI_TIMEOUT_MS
    ) ?: error("Could not find $ANDROID_AUTO_LABEL in the app list")
    item.click()
    check(
      device.wait(
        Until.hasObject(By.res(TARGET_PACKAGE, "tab_layout")),
        UI_TIMEOUT_MS
      )
    ) {
      "App detail page did not open"
    }
  }

  private companion object {
    private const val TARGET_PACKAGE = "com.absinthe.libchecker.debug"
    private const val ANDROID_AUTO_LABEL = "Android Auto"
    private const val UI_TIMEOUT_MS = 15_000L
  }
}
