package com.absinthe.libchecker.macrobenchmark

import android.content.Intent
import android.os.SystemClock
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
class AppListBenchmark {

  @get:Rule
  val benchmarkRule = MacrobenchmarkRule()

  @Test
  fun scrollHomeAppList() = benchmarkRule.measureRepeated(
    packageName = TARGET_PACKAGE,
    metrics = listOf(FrameTimingMetric()),
    compilationMode = CompilationMode.None(),
    iterations = 5,
    setupBlock = {
      requireUncontaminatedBenchmarkEnvironment()
      pressHome()
      startActivityAndWait {
        it.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
      }
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
  ) {
    val width = device.displayWidth
    val height = device.displayHeight
    repeat(SCROLL_REPEAT_COUNT) {
      device.swipe(
        width / 2,
        (height * 0.78f).toInt(),
        width / 2,
        (height * 0.32f).toInt(),
        SCROLL_STEPS
      )
      device.waitForIdle()
    }
    check(
      device.wait(
        Until.hasObject(By.pkg(TARGET_PACKAGE).textStartsWith(PACKAGE_NAME_PREFIX)),
        UI_TIMEOUT_MS
      )
    ) {
      "App list did not keep visible package rows after scrolling"
    }
  }

  @Test
  fun searchGooglePlayServices() = benchmarkRule.measureRepeated(
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
          Until.hasObject(By.res(TARGET_PACKAGE, SEARCH_MENU_RES_ID)),
          UI_TIMEOUT_MS
        )
      ) {
        "App list did not show the search action"
      }
    }
  ) {
    val searchAction = device.wait(
      Until.findObject(By.res(TARGET_PACKAGE, SEARCH_MENU_RES_ID)),
      UI_TIMEOUT_MS
    ) ?: error("Could not find the search action")
    searchAction.click()

    val searchField = device.wait(
      Until.findObject(By.res(TARGET_PACKAGE, SEARCH_TEXT_RES_ID)),
      UI_TIMEOUT_MS
    ) ?: error("Could not find the search text field")
    searchField.setText(GOOGLE_PLAY_SERVICES_QUERY)

    check(
      device.wait(
        Until.hasObject(By.pkg(TARGET_PACKAGE).text(GOOGLE_PLAY_SERVICES_PACKAGE)),
        UI_TIMEOUT_MS
      )
    ) {
      "Search results did not show $GOOGLE_PLAY_SERVICES_PACKAGE"
    }
  }

  @Test
  fun returnFromFilteredAppDetailKeepsFilteredList() = benchmarkRule.measureRepeated(
    packageName = TARGET_PACKAGE,
    metrics = listOf(FrameTimingMetric()),
    compilationMode = CompilationMode.None(),
    iterations = 1,
    setupBlock = {
      pressHome()
      startActivityAndWait {
        it.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
      }
      check(
        device.wait(
          Until.hasObject(By.res(TARGET_PACKAGE, SEARCH_MENU_RES_ID)),
          UI_TIMEOUT_MS
        )
      ) {
        "App list did not show the search action"
      }
    }
  ) {
    val searchAction = device.wait(
      Until.findObject(By.res(TARGET_PACKAGE, SEARCH_MENU_RES_ID)),
      UI_TIMEOUT_MS
    ) ?: error("Could not find the search action")
    searchAction.click()

    val searchField = device.wait(
      Until.findObject(By.res(TARGET_PACKAGE, SEARCH_TEXT_RES_ID)),
      UI_TIMEOUT_MS
    ) ?: error("Could not find the search text field")
    searchField.setText(GOOGLE_PLAY_SERVICES_QUERY)

    val result = device.wait(
      Until.findObject(By.pkg(TARGET_PACKAGE).text(GOOGLE_PLAY_SERVICES_PACKAGE)),
      UI_TIMEOUT_MS
    ) ?: error("Search results did not show $GOOGLE_PLAY_SERVICES_PACKAGE")
    device.waitForIdle()
    val filteredPackages = device.findObjects(
      By.pkg(TARGET_PACKAGE).textStartsWith(PACKAGE_NAME_PREFIX)
    ).mapNotNullTo(linkedSetOf()) { it.text }
    result.click()
    check(
      device.wait(
        Until.hasObject(By.res(TARGET_PACKAGE, APP_DETAIL_TAB_LAYOUT_RES_ID)),
        UI_TIMEOUT_MS
      )
    ) {
      "App detail page did not open"
    }

    device.pressBack()
    val deadline = SystemClock.uptimeMillis() + RETURN_OBSERVATION_MS
    val unexpectedPackages = linkedSetOf<String>()
    while (SystemClock.uptimeMillis() < deadline) {
      device.findObjects(By.pkg(TARGET_PACKAGE).textStartsWith(PACKAGE_NAME_PREFIX))
        .mapNotNullTo(unexpectedPackages) { it.text }
      unexpectedPackages.removeAll(filteredPackages)
    }

    check(unexpectedPackages.isEmpty()) {
      "Returning from detail briefly showed unfiltered packages: $unexpectedPackages"
    }
    check(
      device.wait(
        Until.hasObject(By.pkg(TARGET_PACKAGE).text(GOOGLE_PLAY_SERVICES_PACKAGE)),
        UI_TIMEOUT_MS
      )
    ) {
      "Filtered result was not restored after returning from detail"
    }
  }

  private companion object {
    private const val TARGET_PACKAGE = "com.absinthe.libchecker.debug"
    private const val ANDROID_PACKAGE = "android"
    private const val APP_LIST_RES_ID = "list"
    private const val SEARCH_MENU_RES_ID = "search"
    private const val SEARCH_TEXT_RES_ID = "search_src_text"
    private const val APP_DETAIL_TAB_LAYOUT_RES_ID = "tab_layout"
    private const val PACKAGE_NAME_PREFIX = "com."
    private const val GOOGLE_PLAY_SERVICES_PACKAGE = "com.google.android.gms"
    private const val GOOGLE_PLAY_SERVICES_QUERY = "gms"
    private const val UI_TIMEOUT_MS = 15_000L
    private const val RETURN_OBSERVATION_MS = 1_000L
    private const val SCROLL_REPEAT_COUNT = 4
    private const val SCROLL_STEPS = 24
  }
}
