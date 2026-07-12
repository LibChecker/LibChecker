package com.absinthe.libchecker.domain.app.detail.header

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class DetailHeaderRenderStateTest {

  @Test
  fun matchingRenderIdUpdatesExtraInfoWithoutMutatingOriginalState() {
    val original = renderState(renderId = 7)
    val extraInfo = DetailHeaderExtraInfoState.Android(
      AppDetailHeaderExtraInfo(
        targetSdkInfo = "35",
        minSdkInfo = "24",
        compileSdkInfo = "35",
        sizeInfo = "12 MB",
        sharedUserId = null
      )
    )

    val updated = original.withExtraInfo(renderId = 7, extraInfo = extraInfo)

    assertEquals(DetailHeaderExtraInfoState.Loading, original.extraInfo)
    assertEquals(extraInfo, updated.extraInfo)
  }

  @Test
  fun staleRenderIdCannotReplaceCurrentExtraInfo() {
    val current = renderState(renderId = 8)
    val staleExtraInfo = DetailHeaderExtraInfoState.Harmony(
      targetVersion = "12",
      minSdkVersion = "10",
      jointUserId = "shared"
    )

    val unchanged = current.withExtraInfo(renderId = 7, extraInfo = staleExtraInfo)

    assertSame(current, unchanged)
  }

  private fun renderState(renderId: Int): DetailHeaderRenderState {
    return DetailHeaderRenderState(
      renderId = renderId,
      title = AppDetailHeaderTitleData(
        packageName = "com.example.app",
        appName = "Example",
        title = "Example",
        versionInfo = "1.0 (1)",
        isAppInfoAvailable = true
      )
    )
  }
}
