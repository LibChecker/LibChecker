package com.absinthe.libchecker.domain.snapshot.list.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SnapshotDisplayedChildTest {

  @Test
  fun `new adapter remains loading after an earlier comparison completed`() {
    assertEquals(
      VF_LOADING,
      resolveSnapshotDisplayedChild(
        isListReady = false,
        isComparingActive = false
      )
    )
  }

  @Test
  fun `ready adapter shows list after comparison completed`() {
    assertEquals(
      VF_LIST,
      resolveSnapshotDisplayedChild(
        isListReady = true,
        isComparingActive = false
      )
    )
  }

  @Test
  fun `active comparison keeps ready adapter loading`() {
    assertEquals(
      VF_LOADING,
      resolveSnapshotDisplayedChild(
        isListReady = true,
        isComparingActive = true
      )
    )
  }
}
