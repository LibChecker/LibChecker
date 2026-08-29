package com.absinthe.libchecker.ui.base

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListScreenChromeTest {

  @Test
  fun `second scrollbar drag reveals navigation before the last item`() {
    assertTrue(
      ListScreenChrome.shouldRevealNavigationAfterScrollbarScroll(
        hasSeenScrollbarScroll = true,
        isSearchTextClearOnce = false,
        dx = 0,
        dy = 0,
        lastVisibleItemPosition = 3,
        itemCount = 5,
        isFragmentVisible = true
      )
    )
  }

  @Test
  fun `first scrollbar drag is consumed without revealing navigation`() {
    assertFalse(
      ListScreenChrome.shouldRevealNavigationAfterScrollbarScroll(
        hasSeenScrollbarScroll = false,
        isSearchTextClearOnce = false,
        dx = 0,
        dy = 0,
        lastVisibleItemPosition = 3,
        itemCount = 5,
        isFragmentVisible = true
      )
    )
  }

  @Test
  fun `content scroll does not reveal navigation`() {
    assertFalse(
      ListScreenChrome.shouldRevealNavigationAfterScrollbarScroll(
        hasSeenScrollbarScroll = true,
        isSearchTextClearOnce = false,
        dx = 0,
        dy = 1,
        lastVisibleItemPosition = 3,
        itemCount = 5,
        isFragmentVisible = true
      )
    )
  }
}
