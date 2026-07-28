package com.absinthe.libchecker.domain.app.update

import com.absinthe.libchecker.api.bean.GetAppUpdateInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BuildInAppUpdateDiffDataUseCaseTest {

  @Test
  fun keepsOlderRemoteVersionForDisplayWithoutOfferingUpdate() {
    val stableApp = GetAppUpdateInfo.App(
      version = "2.5.4",
      versionCode = 2671,
      extra = GetAppUpdateInfo.App.Extra(
        target = 35,
        min = 24,
        compile = 35,
        packageSize = 123
      ),
      link = "https://example.com/stable.apk",
      note = null
    )

    val (displayedApp, hasUpdate) = resolveInAppUpdateDisplay(
      displayedRemoteApp = stableApp,
      installableRemoteApp = stableApp,
      localVersionCode = 2771L
    )

    assertEquals(stableApp, displayedApp)
    assertFalse(hasUpdate)
  }

  @Test
  fun displaysOnlineStableVersionWithoutOfferingFossApkToMarketBuild() {
    val stableApp = GetAppUpdateInfo.App(
      version = "2.5.4",
      versionCode = 2671,
      extra = GetAppUpdateInfo.App.Extra(
        target = 37,
        min = 24,
        compile = 37,
        packageSize = 4_779_938
      ),
      link = "https://example.com/foss-stable.apk",
      note = null
    )

    val (displayedApp, hasUpdate) = resolveInAppUpdateDisplay(
      displayedRemoteApp = stableApp,
      installableRemoteApp = null,
      localVersionCode = 2600L
    )

    assertEquals(stableApp, displayedApp)
    assertFalse(hasUpdate)
  }
}
