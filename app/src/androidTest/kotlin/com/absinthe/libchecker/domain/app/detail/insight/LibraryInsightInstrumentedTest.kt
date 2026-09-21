package com.absinthe.libchecker.domain.app.detail.insight

import android.os.Bundle
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.request.RulesDocumentRequest
import com.absinthe.libchecker.data.app.insight.RemoteLibraryInsightRepository
import java.io.File
import kotlinx.coroutines.runBlocking
import okhttp3.Cache
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
    val arguments = InstrumentationRegistry.getArguments()
    val preferGitHub = arguments.getString("preferGitHub") == "true"
    val roots = ApiManager.rulesRootsInPreferenceOrder.let { roots ->
      if (preferGitHub) roots.sortedBy { "githubusercontent.com" !in it } else roots
    }
    // Use the production HTTP settings and cache, changing only source order for comparison.
    val cache = Cache(File(instrumentation.targetContext.cacheDir, "sdk-details-http"), 8L * 1024 * 1024)
    if (arguments.getString("coldHttpCache") == "true") cache.evictAll()
    val client = ApiManager.okHttpClient.newBuilder().cache(cache).build()
    val request = ApiManager.retrofit.newBuilder().client(client).build().create(RulesDocumentRequest::class.java)
    val repository = RemoteLibraryInsightRepository(request) { roots }
    instrumentation.sendStatus(0, Bundle().apply { putString("stream", "roots=$roots\n") })
    var definition: LibraryInsightDefinition? = null
    val definitionPaths = mutableListOf<String>()
    val timedRepository = object : LibraryInsightRepository by repository {
      override suspend fun getCatalog() = timed("catalog") { repository.getCatalog() }
      override suspend fun getDefinition(path: String) = timed(path) {
        definitionPaths += path
        repository.getDefinition(path).also { result ->
          if (result is RemoteDocumentResult.Success) definition = result.value
        }
      }
      override suspend fun getLookup(path: String) = timed(path) { repository.getLookup(path) }
    }
    val resolve = ResolveLibraryInsightUseCase(timedRepository, LibraryInsightDefinitionValidator(), GlobalContext.get().get())
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
    if (preferGitHub) assertEquals(List(2) { FLUTTER_DEFINITION_PATH }, definitionPaths)
    repeat(3) {
      val start = SystemClock.elapsedRealtime()
      assertTrue(LibraryInsightProbeEngine().probe(packageInfo!!, requireNotNull(definition)).evidenceFound)
      instrumentation.sendStatus(
        0,
        Bundle().apply { putString("stream", "uncachedProbeMs=${SystemClock.elapsedRealtime() - start}\n") }
      )
    }
    cache.close()
  }

  private suspend fun <T> timed(stage: String, block: suspend () -> T): T {
    val start = SystemClock.elapsedRealtime()
    return block().also {
      InstrumentationRegistry.getInstrumentation().sendStatus(
        0,
        Bundle().apply { putString("stream", "stage=$stage elapsedMs=${SystemClock.elapsedRealtime() - start}\n") }
      )
    }
  }

  private companion object {
    const val FLUTTER_DEFINITION_PATH = "sdk-details/sdks/flutter/definition.json"
  }
}
