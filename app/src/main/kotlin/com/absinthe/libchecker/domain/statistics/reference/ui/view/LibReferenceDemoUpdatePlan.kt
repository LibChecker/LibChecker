package com.absinthe.libchecker.domain.statistics.reference.ui.view

import com.absinthe.libchecker.domain.statistics.reference.model.LibReference

internal sealed interface LibReferenceDemoUpdatePlan {

  data class AnimateRemoval(val removedIndex: Int) : LibReferenceDemoUpdatePlan

  data class AnimateInsertion(val insertedIndex: Int) : LibReferenceDemoUpdatePlan

  data object ApplyImmediately : LibReferenceDemoUpdatePlan
}

internal class LibReferenceDemoTransitionGate {

  private var generation = 0

  fun advance(): Int = ++generation

  fun isCurrent(generation: Int): Boolean = generation == this.generation
}

internal class LibReferenceDemoTransitionQueue<T> {

  private var isTransitionActive = false
  private var pendingValue: T? = null

  fun offer(value: T): T? {
    if (isTransitionActive) {
      pendingValue = value
      return null
    }
    isTransitionActive = true
    return value
  }

  fun complete(): T? {
    isTransitionActive = false
    return pendingValue.also { pendingValue = null }
  }

  fun clear() {
    isTransitionActive = false
    pendingValue = null
  }
}

internal fun planLibReferenceDemoUpdate(
  currentItems: List<LibReference>,
  nextItems: List<LibReference>
): LibReferenceDemoUpdatePlan {
  if (currentItems.size == nextItems.size + 1) {
    currentItems.indices.forEach { removedIndex ->
      val remainingItems = currentItems.filterIndexed { index, _ -> index != removedIndex }
      if (remainingItems.sameDemoSequence(nextItems)) {
        return LibReferenceDemoUpdatePlan.AnimateRemoval(removedIndex)
      }
    }
  }
  if (nextItems.size == currentItems.size + 1) {
    nextItems.indices.forEach { insertedIndex ->
      val previousItems = nextItems.filterIndexed { index, _ -> index != insertedIndex }
      if (previousItems.sameDemoSequence(currentItems)) {
        return LibReferenceDemoUpdatePlan.AnimateInsertion(insertedIndex)
      }
    }
  }
  return LibReferenceDemoUpdatePlan.ApplyImmediately
}

private fun List<LibReference>.sameDemoSequence(other: List<LibReference>): Boolean {
  return size == other.size && zip(other).all { (current, next) ->
    current.libName == next.libName && current.type == next.type
  }
}
