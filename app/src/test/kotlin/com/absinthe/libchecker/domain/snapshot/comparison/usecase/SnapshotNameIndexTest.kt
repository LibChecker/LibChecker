package com.absinthe.libchecker.domain.snapshot.comparison.usecase

import com.absinthe.libchecker.domain.app.detail.model.LibStringItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SnapshotNameIndexTest {
  @Test
  fun remainingDuplicateRowsKeepTheirOrderAndOriginalValues() {
    val first = LibStringItem("a", 1)
    val second = LibStringItem("a", 2)
    val other = LibStringItem("b", 3)
    val index = SnapshotNameIndex(listOf(first, other, second))
    assertEquals(first, index.match("a"))
    assertNull(index.match("missing"))
    assertEquals(listOf(other, second), index.remainingItems())
    assertEquals(first, index.match("a"))
    assertEquals(first, index.match("a"))
    assertEquals(listOf(other), index.remainingItems())
  }
}
