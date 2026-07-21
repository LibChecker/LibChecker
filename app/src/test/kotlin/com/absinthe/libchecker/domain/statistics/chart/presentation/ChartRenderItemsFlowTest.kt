package com.absinthe.libchecker.domain.statistics.chart.presentation

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class ChartRenderItemsFlowTest {

  @Test
  fun `initial item emits without violating flow context`() = runBlocking {
    val collected = mutableListOf<Int>()
    val collection = launch {
      flowOf(1)
        .debounceSubsequent(delayMillis = 10)
        .take(1)
        .toList(collected)
    }

    withTimeout(1_000) {
      collection.join()
    }

    assertEquals(listOf(1), collected)
  }

  @Test
  fun `subsequent items are debounced with latest value winning`() = runBlocking {
    val upstream = MutableSharedFlow<Int>(replay = 1)
    val collected = mutableListOf<Int>()
    val initialItemCollected = CompletableDeferred<Unit>()
    upstream.emit(1)
    val collection = launch {
      upstream.debounceSubsequent(delayMillis = 50)
        .onEach { value ->
          if (value == 1) {
            initialItemCollected.complete(Unit)
          }
        }
        .take(2)
        .toList(collected)
    }

    withTimeout(1_000) {
      initialItemCollected.await()
    }
    upstream.emit(2)
    delay(10)
    upstream.emit(3)
    withTimeout(1_000) {
      collection.join()
    }

    assertEquals(listOf(1, 3), collected)
  }
}
