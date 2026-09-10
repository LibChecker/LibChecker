package com.absinthe.libchecker.domain.statistics.reference.model

import com.absinthe.libchecker.annotation.NATIVE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LibReferenceTreemapDataTest {
  @Test
  fun groupsAtMostFiveReferencesWithoutDeduplicatingApps() {
    val small = (1..5).map { reference("small$it", it) }
    val large = reference("six", 6)
    val items = buildLibReferenceTreemapData(small + large)
    assertEquals(2, items.size)
    assertSame(large, items.first().reference)
    assertEquals(6L, items.first().count)
    val other = items.last()
    assertNull(other.reference)
    assertEquals(5, other.libraryCount)
    assertEquals(15L, other.count)
    assertEquals("other", other.id)
    assertEquals(other, buildLibReferenceTreemapData(small.reversed()).single())
  }

  @Test
  fun aggregationUsesOnlySuppliedFilteredReferences() {
    val references = listOf(reference("one", 1), reference("three", 3), reference("six", 6))
    val filtered = buildLibReferenceTreemapData(references.filter { it.libName == "three" })
    assertEquals(1, filtered.size)
    assertNull(filtered.single().reference)
    assertEquals(3L, filtered.single().count)
    assertEquals(1, filtered.single().libraryCount)
    assertTrue(buildLibReferenceTreemapData(emptyList()).isEmpty())
  }

  private fun reference(name: String, count: Int): LibReference = LibReference(
    libName = name,
    rule = null,
    referredList = (1..count).map { "app$it" }.toSet(),
    type = NATIVE
  )
}
