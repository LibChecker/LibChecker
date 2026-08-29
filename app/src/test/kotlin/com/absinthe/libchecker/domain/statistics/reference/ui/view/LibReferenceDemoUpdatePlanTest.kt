package com.absinthe.libchecker.domain.statistics.reference.ui.view

import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.domain.statistics.reference.model.LibReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibReferenceDemoUpdatePlanTest {

  @Test
  fun `single removal keeps the old list while its row collapses`() {
    val currentItems = listOf(reference("service", SERVICE), reference("activity", ACTIVITY), reference("provider", PROVIDER))
    val nextItems = listOf(currentItems[0], currentItems[2])

    assertEquals(
      LibReferenceDemoUpdatePlan.AnimateRemoval(removedIndex = 1),
      planLibReferenceDemoUpdate(currentItems, nextItems)
    )
  }

  @Test
  fun `single insertion enters at zero height before its row expands`() {
    val currentItems = listOf(reference("service", SERVICE), reference("provider", PROVIDER))
    val nextItems = listOf(currentItems[0], reference("activity", ACTIVITY), currentItems[1])

    assertEquals(
      LibReferenceDemoUpdatePlan.AnimateInsertion(insertedIndex = 1),
      planLibReferenceDemoUpdate(currentItems, nextItems)
    )
  }

  @Test
  fun `a newer render invalidates callbacks captured by an older transition`() {
    val gate = LibReferenceDemoTransitionGate()
    val staleGeneration = gate.advance()
    val currentGeneration = gate.advance()

    assertFalse(gate.isCurrent(staleGeneration))
    assertTrue(gate.isCurrent(currentGeneration))
  }

  @Test
  fun `multiple changes bypass a stale single row transition`() {
    val currentItems = listOf(reference("service", SERVICE), reference("activity", ACTIVITY))

    assertEquals(
      LibReferenceDemoUpdatePlan.ApplyImmediately,
      planLibReferenceDemoUpdate(currentItems, emptyList())
    )
  }

  @Test
  fun `active transition keeps only the latest queued render`() {
    val queue = LibReferenceDemoTransitionQueue<String>()

    assertEquals("initial", queue.offer("initial"))
    assertNull(queue.offer("intermediate"))
    assertNull(queue.offer("latest"))
    assertEquals("latest", queue.complete())
    assertEquals("next", queue.offer("next"))
  }

  private fun reference(name: String, type: Int) = LibReference(
    libName = name,
    rule = null,
    referredList = emptySet(),
    type = type
  )
}
