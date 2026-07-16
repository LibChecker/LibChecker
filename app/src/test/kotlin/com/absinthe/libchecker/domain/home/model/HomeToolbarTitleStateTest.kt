package com.absinthe.libchecker.domain.home.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeToolbarTitleStateTest {

  @Test
  fun `loading update keeps prepared title`() {
    val title = StringBuilder("LibChecker")
    val initial = HomeToolbarTitleState(title = title)

    val loading = initial.withLoading(true)

    assertSame(title, loading.title)
    assertTrue(loading.isLoading)
    assertFalse(initial.isLoading)
  }

  @Test
  fun `unchanged loading value reuses state`() {
    val state = HomeToolbarTitleState(title = "LibChecker", isLoading = true)

    assertSame(state, state.withLoading(true))
  }
}
