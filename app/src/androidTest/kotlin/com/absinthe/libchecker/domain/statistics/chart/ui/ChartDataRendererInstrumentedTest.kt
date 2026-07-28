package com.absinthe.libchecker.domain.statistics.chart.ui

import android.view.View
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.domain.statistics.chart.model.LOADING_PROGRESS_MAX
import java.util.Collections
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChartDataRendererInstrumentedTest {

  @Test
  fun terminalProgressIsReportedAfterChartCommit() = runBlocking {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val terminalProgressReported = CompletableDeferred<Unit>()
    val events = Collections.synchronizedList(mutableListOf<String>())

    withContext(Dispatchers.Main) {
      val chartHost = FrameLayout(context)
      val renderer = ChartDataRenderer(scope, chartHost) { progress ->
        events += "progress:$progress"
        if (progress == LOADING_PROGRESS_MAX) {
          terminalProgressReported.complete(Unit)
        }
      }
      renderer.render(
        newChartView = View(context),
        fillChart = { _, onProgress -> onProgress(LOADING_PROGRESS_MAX) },
        onCommitted = { events += "committed" }
      )
    }

    terminalProgressReported.await()
    assertEquals(
      listOf("committed", "progress:$LOADING_PROGRESS_MAX"),
      events
    )
    scope.cancel()
  }

  @Test
  fun latestRenderAtomicallyOwnsChartHost() = runBlocking {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val firstRenderStarted = CompletableDeferred<Unit>()
    val releaseFirstRender = CompletableDeferred<Unit>()
    val latestRenderCommitted = CompletableDeferred<Unit>()
    val reportedProgress = Collections.synchronizedList(mutableListOf<Int>())
    lateinit var chartHost: FrameLayout
    lateinit var initialChart: View
    lateinit var staleChart: View
    lateinit var latestChart: View
    lateinit var renderer: ChartDataRenderer

    withContext(Dispatchers.Main) {
      chartHost = FrameLayout(context)
      initialChart = View(context)
      staleChart = View(context)
      latestChart = View(context)
      renderer = ChartDataRenderer(scope, chartHost) { reportedProgress += it }
      renderer.showInitialChart(initialChart)
      renderer.render(
        newChartView = staleChart,
        fillChart = { _, onProgress ->
          firstRenderStarted.complete(Unit)
          withContext(NonCancellable) {
            releaseFirstRender.await()
            onProgress(STALE_PROGRESS)
          }
        },
        onCommitted = {}
      )
    }

    firstRenderStarted.await()
    withContext(Dispatchers.Main) {
      assertSame(initialChart, chartHost.getChildAt(0))
      renderer.render(
        newChartView = latestChart,
        fillChart = { _, onProgress -> onProgress(LATEST_PROGRESS) },
        onCommitted = { latestRenderCommitted.complete(Unit) }
      )
    }

    latestRenderCommitted.await()
    releaseFirstRender.complete(Unit)
    delay(STALE_RENDER_SETTLE_MILLIS)

    withContext(Dispatchers.Main) {
      assertEquals(1, chartHost.childCount)
      assertSame(latestChart, chartHost.getChildAt(0))
    }
    assertTrue(LATEST_PROGRESS in reportedProgress)
    assertTrue(STALE_PROGRESS !in reportedProgress)
    scope.cancel()
  }

  private companion object {
    const val LATEST_PROGRESS = 20
    const val STALE_PROGRESS = 90
    const val STALE_RENDER_SETTLE_MILLIS = 100L
  }
}
