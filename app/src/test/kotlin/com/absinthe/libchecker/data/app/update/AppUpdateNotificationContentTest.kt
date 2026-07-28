package com.absinthe.libchecker.data.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppUpdateNotificationContentTest {

  @Test
  fun buildsPreviousAndCurrentVersionsLikeSnapshotDiff() {
    val previous = AppUpdateNotificationVersionInfo(
      versionName = "2.5.5",
      versionCode = 2699
    )
    val current = AppUpdateNotificationVersionInfo(
      versionName = "2.5.6",
      versionCode = 2700
    )

    assertEquals(
      "2.5.5 (2699) → 2.5.6 (2700)",
      buildAppUpdateNotificationContent(previous, current)
    )
  }

  @Test
  fun omitsContentWhenEitherAppVersionIsUnavailable() {
    val current = AppUpdateNotificationVersionInfo(
      versionName = "2.5.6",
      versionCode = 2700
    )

    assertNull(buildAppUpdateNotificationContent(previous = null, current = current))
    assertNull(buildAppUpdateNotificationContent(previous = current, current = null))
  }
}
