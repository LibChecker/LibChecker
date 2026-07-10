package com.absinthe.libchecker.domain.settings.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GetUpdatesDialogStateTest {

  @Test
  fun `dialog state keeps ordered update destinations and actions`() {
    val github = GetUpdatesItem(
      text = "GitHub",
      iconRes = 1,
      action = GetUpdatesAction.OpenUri("https://github.com/LibChecker/LibChecker")
    )
    val inApp = GetUpdatesItem(
      text = "In-app update",
      iconRes = 2,
      action = GetUpdatesAction.OpenInAppUpdate
    )

    val state = GetUpdatesDialogState(listOf(github, inApp))

    assertEquals(listOf(github, inApp), state.items)
  }
}
