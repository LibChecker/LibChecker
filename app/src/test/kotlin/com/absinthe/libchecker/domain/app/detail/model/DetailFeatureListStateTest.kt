package com.absinthe.libchecker.domain.app.detail.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailFeatureListStateTest {

  @Test
  fun defaultStateHasNoItemsOrLoading() {
    val state = DetailFeatureListState()

    assertEquals(emptyList<FeatureItem>(), state.items)
    assertFalse(state.isLoading)
  }

  @Test
  fun itemWithoutPositionIsAppended() {
    val first = featureItem(1)
    val second = featureItem(2)

    val state = DetailFeatureListState(items = listOf(first))
      .withItem(DetailFeatureItem(second))

    assertEquals(listOf(first, second), state.items)
  }

  @Test
  fun positionedItemIsInsertedWithoutMutatingExistingState() {
    val first = featureItem(1)
    val second = featureItem(2)
    val priority = featureItem(3)
    val original = DetailFeatureListState(items = listOf(first, second))

    val updated = original.withItem(DetailFeatureItem(priority, position = 1))

    assertEquals(listOf(first, second), original.items)
    assertEquals(listOf(first, priority, second), updated.items)
  }

  @Test
  fun staleInsertionPositionFallsBackToListEnd() {
    val first = featureItem(1)
    val late = featureItem(2)

    val state = DetailFeatureListState(items = listOf(first))
      .withItem(DetailFeatureItem(late, position = 10))

    assertEquals(listOf(first, late), state.items)
  }

  @Test
  fun loadingStatePreservesItems() {
    val item = featureItem(1)

    val state = DetailFeatureListState(items = listOf(item)).copy(isLoading = true)

    assertEquals(listOf(item), state.items)
    assertTrue(state.isLoading)
  }

  private fun featureItem(res: Int): FeatureItem {
    return FeatureItem(
      res = res,
      action = {}
    )
  }
}
