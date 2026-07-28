package com.absinthe.libchecker.domain.app.list.ui.adapter

import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.domain.app.list.model.AppListRenderState
import org.junit.Assert.assertTrue
import org.junit.Test

class AppAdapterTest {

  @Test
  fun `render state change refreshes existing demo item`() {
    val previousState = AppListRenderState()
    val newState = AppListRenderState(
      fallbackDisplayOptions = AdvancedOptions.DEFAULT_OPTIONS xor
        AdvancedOptions.SHOW_TARGET_API
    )

    assertTrue(
      shouldRefreshAppAdapterItems(
        previousState = previousState,
        newState = newState,
        refreshItems = true,
        itemCount = 1
      )
    )
  }
}
