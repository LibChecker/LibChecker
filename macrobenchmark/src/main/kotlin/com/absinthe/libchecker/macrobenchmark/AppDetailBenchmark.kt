package com.absinthe.libchecker.macrobenchmark

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
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

  @Test
  fun openGooglePlayServicesDetail() = benchmarkRule.measureRepeated(
    packageName = TARGET_PACKAGE,
    metrics = listOf(FrameTimingMetric()),
    compilationMode = CompilationMode.None(),
    iterations = 5,
    setupBlock = {
      pressHome()
    }
  ) {
    openGooglePlayServicesDetailScreen()
    waitForTextStartsWith(NATIVE_LIBRARY_PREFIX, "native library list")
  }

  @Test
  fun openYouTubeDetail() = benchmarkRule.measureRepeated(
    packageName = TARGET_PACKAGE,
    metrics = listOf(FrameTimingMetric()),
    compilationMode = CompilationMode.None(),
    iterations = 5,
    setupBlock = {
      pressHome()
    }
  ) {
    openPackageDetailScreen(
      packageName = YOUTUBE_PACKAGE,
      label = YOUTUBE_LABEL
    )
    waitForTextStartsWith(NATIVE_LIBRARY_PREFIX, "native library list")
  }

  @Test
  fun switchGooglePlayServicesDetailTabs() = benchmarkRule.measureRepeated(
    packageName = TARGET_PACKAGE,
    metrics = listOf(FrameTimingMetric()),
    compilationMode = CompilationMode.None(),
    iterations = 5,
    setupBlock = {
      pressHome()
    }
  ) {
    openGooglePlayServicesDetailScreen()
    waitForTextStartsWith(NATIVE_LIBRARY_PREFIX, "native library list")

    selectTab(SERVICE_TAB_TITLE)
    waitForTextContains(SERVICE_CONTENT_TEXT, "service list")

    selectTab(ACTIVITY_TAB_TITLE)
    waitForTextContains(ACTIVITY_CONTENT_TEXT, "activity list")

    selectTab(RECEIVER_TAB_TITLE)
    waitForTextContains(RECEIVER_CONTENT_TEXT, "receiver list")

    selectTab(PROVIDER_TAB_TITLE)
    waitForTextContains(PROVIDER_CONTENT_TEXT, "provider list")

    selectTab(PERMISSION_TAB_TITLE)
    waitForTextContains(PERMISSION_CONTENT_TEXT, "permission list")

    selectTab(METADATA_TAB_TITLE)
    waitForTextContains(METADATA_CONTENT_TEXT, "metadata list")
  }

  private fun MacrobenchmarkScope.openGooglePlayServicesDetailScreen() {
    openPackageDetailScreen(
      packageName = GOOGLE_PLAY_SERVICES_PACKAGE,
      label = GOOGLE_PLAY_SERVICES_LABEL
    )
  }

  private fun MacrobenchmarkScope.openPackageDetailScreen(
    packageName: String,
    label: String
  ) {
    startActivityAndWait(
      Intent(Intent.ACTION_SHOW_APP_INFO).apply {
        setClassName(TARGET_PACKAGE, APP_DETAIL_ACTIVITY)
        putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
        flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
      }
    )
    waitForText(label, "app label")
    waitForText(packageName, "package name")
    check(
      device.wait(
        Until.hasObject(By.res(TARGET_PACKAGE, "tab_layout")),
        UI_TIMEOUT_MS
      )
    ) {
      "App detail page did not open"
    }
  }

  private fun MacrobenchmarkScope.selectTab(tabTitle: String) {
    val tab = device.wait(
      Until.findObject(By.pkg(TARGET_PACKAGE).desc(tabTitle)),
      UI_TIMEOUT_MS
    ) ?: error("Could not find the $tabTitle tab")
    tab.click()
    device.waitForIdle()
  }

  private fun MacrobenchmarkScope.waitForText(text: String, description: String) {
    check(
      device.wait(
        Until.hasObject(By.pkg(TARGET_PACKAGE).text(text)),
        UI_TIMEOUT_MS
      )
    ) {
      "App detail page did not show $description"
    }
  }

  private fun MacrobenchmarkScope.waitForTextStartsWith(prefix: String, description: String) {
    check(
      device.wait(
        Until.hasObject(By.pkg(TARGET_PACKAGE).textStartsWith(prefix)),
        UI_TIMEOUT_MS
      )
    ) {
      "App detail page did not show $description"
    }
  }

  private fun MacrobenchmarkScope.waitForTextContains(text: String, description: String) {
    check(
      device.wait(
        Until.hasObject(By.pkg(TARGET_PACKAGE).textContains(text)),
        UI_TIMEOUT_MS
      )
    ) {
      "App detail page did not show $description"
    }
  }

  private companion object {
    private const val TARGET_PACKAGE = "com.absinthe.libchecker.debug"
    private const val APP_DETAIL_ACTIVITY = "com.absinthe.libchecker.domain.app.detail.ui.AppDetailActivity"
    private const val ANDROID_AUTO_LABEL = "Android Auto"
    private const val GOOGLE_PLAY_SERVICES_LABEL = "Google Play 服务"
    private const val GOOGLE_PLAY_SERVICES_PACKAGE = "com.google.android.gms"
    private const val YOUTUBE_LABEL = "YouTube"
    private const val YOUTUBE_PACKAGE = "com.google.android.youtube"
    private const val NATIVE_LIBRARY_PREFIX = "lib"
    private const val SERVICE_TAB_TITLE = "服务"
    private const val ACTIVITY_TAB_TITLE = "活动"
    private const val RECEIVER_TAB_TITLE = "广播接收器"
    private const val PROVIDER_TAB_TITLE = "内容提供器"
    private const val PERMISSION_TAB_TITLE = "权限"
    private const val METADATA_TAB_TITLE = "元数据"
    private const val SERVICE_CONTENT_TEXT = "Service"
    private const val ACTIVITY_CONTENT_TEXT = "Activity"
    private const val RECEIVER_CONTENT_TEXT = "Receiver"
    private const val PROVIDER_CONTENT_TEXT = "Provider"
    private const val PERMISSION_CONTENT_TEXT = "permission"
    private const val METADATA_CONTENT_TEXT = "stamp"
    private const val UI_TIMEOUT_MS = 15_000L
  }
}
