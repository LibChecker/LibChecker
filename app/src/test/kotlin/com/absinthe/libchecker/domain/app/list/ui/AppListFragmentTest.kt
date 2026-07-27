package com.absinthe.libchecker.domain.app.list.ui

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
}
