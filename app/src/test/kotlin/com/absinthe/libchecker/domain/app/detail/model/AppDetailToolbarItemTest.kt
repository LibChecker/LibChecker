package com.absinthe.libchecker.domain.app.detail.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppDetailToolbarItemTest {

  @Test
  fun stableIdDoesNotChangeWithVisibleLabel() {
    val item = AppDetailToolbarItem(
      action = AppDetailToolbarAction.PROCESS,
      iconRes = 1,
      label = "Processes"
    )

    assertEquals(item.stableId, item.copy(label = "Running processes").stableId)
  }

  @Test
  fun actionsHaveDistinctStableIds() {
    val ids = AppDetailToolbarAction.entries.map { action ->
      AppDetailToolbarItem(action, iconRes = 1, label = action.name).stableId
    }

    assertEquals(AppDetailToolbarAction.entries.size, ids.distinct().size)
  }
}
