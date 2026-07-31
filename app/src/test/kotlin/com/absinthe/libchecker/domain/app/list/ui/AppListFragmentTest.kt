package com.absinthe.libchecker.domain.app.list.ui

import androidx.lifecycle.Lifecycle
import com.absinthe.libchecker.domain.home.presentation.HomeViewModel
import com.absinthe.libchecker.ui.base.InitialListSearchState
import com.absinthe.libchecker.ui.base.initialListSearchState
import com.absinthe.libchecker.ui.base.shouldHandleListSearchQueryChange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppListFragmentTest {

  @Test
  fun `clearing search returns restored app list to top`() {
    assertTrue(
      shouldReturnAppListTopAfterSearch(
        previousQuery = "壁纸",
        newQuery = ""
      )
    )
  }

  @Test
  fun `typing search does not force app list to top`() {
    assertFalse(
      shouldReturnAppListTopAfterSearch(
        previousQuery = "壁",
        newQuery = "壁纸"
      )
    )
  }

  @Test
  fun `restores retained query and expands search after returning to list`() {
    assertEquals(
      InitialListSearchState(
        query = "aida",
        shouldExpand = true
      ),
      initialListSearchState(
        retainedQuery = "aida",
        toolbarState = HomeViewModel.ToolbarSearchMenuState()
      )
    )
  }

  @Test
  fun `retained query remains authoritative over stale toolbar query`() {
    assertEquals(
      InitialListSearchState(
        query = "chrome",
        shouldExpand = true
      ),
      initialListSearchState(
        retainedQuery = "chrome",
        toolbarState = HomeViewModel.ToolbarSearchMenuState(
          isExpanded = true,
          query = "aida"
        )
      )
    )
  }

  @Test
  fun `restores empty expanded search without replaying toolbar query`() {
    assertEquals(
      InitialListSearchState(
        query = "",
        shouldExpand = true
      ),
      initialListSearchState(
        retainedQuery = "",
        toolbarState = HomeViewModel.ToolbarSearchMenuState(
          isExpanded = true,
          query = "aida"
        )
      )
    )
  }

  @Test
  fun `handles search changes while app list is resumed`() {
    assertTrue(shouldHandleListSearchQueryChange(Lifecycle.State.RESUMED))
  }

  @Test
  fun `ignores search clearing when app list menu is paused`() {
    assertFalse(shouldHandleListSearchQueryChange(Lifecycle.State.STARTED))
  }
}
