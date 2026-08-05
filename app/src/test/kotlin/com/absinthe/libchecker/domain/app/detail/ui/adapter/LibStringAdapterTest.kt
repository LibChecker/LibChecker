package com.absinthe.libchecker.domain.app.detail.ui.adapter

import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.domain.app.detail.model.LibStringRenderState
import org.junit.Assert.assertTrue
import org.junit.Test

class LibStringAdapterTest {

  @Test
  fun `render state change refreshes existing demo items`() {
    val previousState = LibStringRenderState()
    val newState = previousState.copy(
      itemDisplayOptions = previousState.itemDisplayOptions xor AdvancedOptions.MARK_EXPORTED
    )

    assertTrue(
      shouldRefreshLibStringAdapterItems(
        previousState = previousState,
        newState = newState,
        refreshItems = true,
        itemCount = 3
      )
    )
  }
}
