package com.absinthe.libchecker.domain.app.detail.model

import android.content.Intent
import com.absinthe.libchecker.domain.app.detail.action.AppInfoLaunchAction
import com.absinthe.libchecker.domain.app.detail.action.AppInfoPrimaryActions
import com.absinthe.libchecker.domain.app.detail.action.AppLaunchAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class AppInfoBottomSheetStateTest {

  @Test
  fun `loading state keeps primary actions available without preloaded data`() {
    val state = AppInfoBottomSheetState.Loading("sample.package")

    assertEquals(
      AppInfoBottomSheetAction.Launch("sample.package", preloadedAction = null),
      state.launchAction()
    )
    assertEquals("sample.package", state.settingsAction().packageName)
    assertNull(state.settingsAction().preloadedIntent)
    assertNull(state.launcherActivityCopyText)
    assertEquals(emptyList<AppInfoActionItem>(), state.externalActions)
  }

  @Test
  fun `content state reuses prepared actions and external items`() {
    val launchIntent = Intent()
    val settingsIntent = Intent()
    val externalIntent = Intent()
    val launchAction = AppInfoLaunchAction.Available(
      AppLaunchAction(
        launcherActivity = "sample.package.MainActivity",
        intent = launchIntent
      )
    )
    val externalItem = AppInfoActionItem(
      packageName = "external.package",
      label = "External",
      icon = null,
      intent = externalIntent
    )
    val state = AppInfoBottomSheetState.Content(
      packageName = "sample.package",
      primaryActions = AppInfoPrimaryActions(
        launchAction = launchAction,
        settingsIntent = settingsIntent
      ),
      externalActions = listOf(externalItem)
    )

    assertSame(launchAction, state.launchAction().preloadedAction)
    assertSame(settingsIntent, state.settingsAction().preloadedIntent)
    assertEquals("sample.package.MainActivity", state.launcherActivityCopyText)
    assertEquals(listOf(externalItem), state.externalActions)
  }
}
