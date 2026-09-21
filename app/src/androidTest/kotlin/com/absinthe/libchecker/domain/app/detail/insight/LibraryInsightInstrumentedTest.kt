package com.absinthe.libchecker.domain.app.detail.insight

import android.os.Bundle
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeNotNull
import org.junit.Test
import org.koin.core.context.GlobalContext

/** Online smoke using LocalSend's real native library and the production HTTP cache. */
class LibraryInsightInstrumentedTest {
  @Test
  fun resolvesLocalSendOnRepeatedQueries() = runBlocking {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val packageInfo = runCatching {
      instrumentation.targetContext.packageManager.getPackageInfo("org.localsend.localsend_app", 0)
    }.getOrNull()
    assumeNotNull(packageInfo)
    val resolve = GlobalContext.get().get<ResolveLibraryInsightUseCase>()
    var first: LibraryInsightResult? = null
    repeat(2) { iteration ->
      val start = SystemClock.elapsedRealtime()
      val result = resolve("AEF9680F-4A43-4EDC-A5B8-8119D23BCD21", packageInfo!!, "en")
      assertTrue("Expected Flutter content, got $result", result is LibraryInsightResult.Content)
      val content = (result as LibraryInsightResult.Content).content
      assertTrue(content.summary.any { it.label == "Flutter" && it.values.isNotEmpty() })
      if (first == null) first = result else assertEquals(first, result)
      instrumentation.sendStatus(
        0,
        Bundle().apply {
          putString("stream", "query=$iteration elapsedMs=${SystemClock.elapsedRealtime() - start} summary=${content.summary}\n")
        }
      )
    }
    val repository = GlobalContext.get().get<LibraryInsightRepository>()
    val definition = (repository.getDefinition("sdk-details/candidates/flutter/definition.json") as RemoteDocumentResult.Success).value
    val start = SystemClock.elapsedRealtime()
    assertTrue(LibraryInsightProbeEngine().probe(packageInfo!!, definition).evidenceFound)
    instrumentation.sendStatus(
      0,
      Bundle().apply { putString("stream", "uncachedProbeMs=${SystemClock.elapsedRealtime() - start}\n") }
    )
  }
}
