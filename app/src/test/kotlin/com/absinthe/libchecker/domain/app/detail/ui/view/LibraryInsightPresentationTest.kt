package com.absinthe.libchecker.domain.app.detail.ui.view

import com.absinthe.libchecker.domain.app.detail.insight.LibraryInsightContent
import com.absinthe.libchecker.domain.app.detail.insight.LibraryInsightField
import com.absinthe.libchecker.domain.app.detail.insight.LibraryInsightUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryInsightPresentationTest {

  @Test
  fun `detection stays hidden until content is ready`() {
    assertSame(
      LibraryInsightUiState.Hidden,
      LibraryInsightUiState.Loading.toDeferredDisplayState()
    )
  }

  @Test
  fun `detected content remains visible`() {
    val state = LibraryInsightUiState.Content(
      LibraryInsightContent(
        sdkId = "example-sdk",
        summary = listOf(
          LibraryInsightField(
            label = "Version",
            values = listOf("1.2.3"),
            totalCount = 1
          )
        ),
        details = emptyList()
      )
    )

    assertSame(state, state.toDeferredDisplayState())
    assertTrue(state.shouldAnimateReveal(isCurrentlyGone = true, isContainerLaidOut = true))
  }

  @Test
  fun `unavailable result does not play version reveal animation`() {
    assertFalse(
      LibraryInsightUiState.Unavailable.shouldAnimateReveal(
        isCurrentlyGone = true,
        isContainerLaidOut = true
      )
    )
  }
}
