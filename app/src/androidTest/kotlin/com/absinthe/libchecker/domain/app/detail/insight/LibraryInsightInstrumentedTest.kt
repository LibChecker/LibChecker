package com.absinthe.libchecker.domain.app.detail.insight

import android.app.Instrumentation
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.util.Base64
import android.view.InputDevice
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.descendants
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.request.RulesDocumentRequest
import com.absinthe.libchecker.data.app.insight.RemoteLibraryInsightRepository
import com.absinthe.libchecker.database.RulesRepository
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.ui.AppDetailActivity
import com.absinthe.libchecker.domain.app.detail.ui.dialog.LibDetailDialogFragment
import com.absinthe.libchecker.domain.app.repository.LibraryDetailRepository
import com.absinthe.libchecker.domain.statistics.reference.ui.EXTRA_REF_NAME
import com.absinthe.libchecker.domain.statistics.reference.ui.EXTRA_REF_TYPE
import com.absinthe.libchecker.utils.JsonUtil
import com.absinthe.libchecker.view.app.RuleLoadingView
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.runBlocking
import okhttp3.Cache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeNotNull
import org.junit.Test
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

class LibraryInsightInstrumentedTest {
  @Test
  fun showsVersionAfterOpeningNativeLibraryRow() {
    val arguments = InstrumentationRegistry.getArguments()
    val target = arguments.getString("targetPackage")
    val expected = arguments.getString("expectedVersion")
    assumeNotNull(target, expected)
    val version = requireNotNull(expected)
    val library = arguments.getString("targetLibrary") ?: "libyuanshen.so"
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    // The reference-list entry point opens the native tab and scrolls the named row into view.
    val intent = Intent(instrumentation.targetContext, AppDetailActivity::class.java)
      .putExtra(EXTRA_PACKAGE_NAME, target)
      .putExtra(EXTRA_REF_TYPE, NATIVE)
      .putExtra(EXTRA_REF_NAME, library)
    ActivityScenario.launch<AppDetailActivity>(intent).use {
      var listWindow: Int? = null
      val deadline = SystemClock.elapsedRealtime() + 30_000
      do {
        val window = instrumentation.uiAutomation.rootInActiveWindow
        if (window != null && window.packageName == instrumentation.targetContext.packageName) {
          val matches = window.findAccessibilityNodeInfosByText(library).filter { node -> node.isVisibleToUser }
          val versions = window.findAccessibilityNodeInfosByText(version).filter { node -> node.isVisibleToUser }
          if (listWindow == null) {
            val row = matches.firstOrNull { node -> node.isClickable }
            if (row != null) {
              assertFalse("Version appeared on the app page before the row was opened", versions.isNotEmpty())
              val bounds = Rect().also(row::getBoundsInScreen)
              listWindow = window.windowId
              instrumentation.tapScreen(bounds.exactCenterX(), bounds.exactCenterY())
            }
          } else if (window.windowId != listWindow && matches.isNotEmpty() && versions.isNotEmpty()) {
            return
          }
        }
        SystemClock.sleep(100)
      } while (SystemClock.elapsedRealtime() < deadline)
      throw AssertionError("Expected $version in the $library dialog for $target (row tapped: ${listWindow != null})")
    }
  }

  private fun Instrumentation.tapScreen(x: Float, y: Float) {
    val down = SystemClock.uptimeMillis()
    for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
      val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, y, 0)
      try {
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        sendPointerSync(event)
      } finally {
        event.recycle()
      }
    }
  }

  /** A known Library rule can have no description while its native-name SDK probe still succeeds. */
  @Test
  fun firstOpenWithMissingDescriptionFinishesLoading() {
    val arguments = InstrumentationRegistry.getArguments()
    val target = arguments.getString("targetPackage")
    val version = arguments.getString("expectedVersion")
    assumeNotNull(target, version)
    val library = arguments.getString("targetLibrary") ?: "libil2cpp.so"
    val definition = candidateDefinition(arguments).copy(nativeLibraryNames = listOf(library))
    val known = runBlocking { RulesRepository.getRule(library, NATIVE, true) != null }
    assertTrue("Expected a locally recognized native library: $library", known)

    val koin = GlobalContext.get()
    val originalDetails = koin.get<LibraryDetailRepository>()
    var originalInsights: LibraryInsightRepository? = null
    val missingRequested = AtomicBoolean()
    val missing = object : LibraryDetailRepository by originalDetails {
      override suspend fun requestLibraryDetail(categoryDir: String, libPath: String): Nothing {
        check(categoryDir == "native-libs" && libPath == library)
        missingRequested.set(true)
        throw FileNotFoundException(libPath)
      }
    }
    try {
      loadKoinModules(module { single<LibraryDetailRepository> { missing } })
      val context = InstrumentationRegistry.getInstrumentation().targetContext
      val intent = Intent(context, AppDetailActivity::class.java).putExtra(EXTRA_PACKAGE_NAME, target)
      ActivityScenario.launch<AppDetailActivity>(intent).use { scenario ->
        originalInsights = koin.get<LibraryInsightRepository>()
        loadKoinModules(module { single<LibraryInsightRepository> { stagedRepository(definition) } })
        var firstDialog: LibDetailDialogFragment? = null
        val deadline = SystemClock.elapsedRealtime() + 15_000
        do {
          var visible = false
          scenario.onActivity { activity ->
            if (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) || activity.supportFragmentManager.isStateSaved) return@onActivity
            if (firstDialog == null && ViewModelProvider(activity)[DetailViewModel::class.java].isPackageInfoAvailable()) {
              firstDialog = LibDetailDialogFragment.newInstance(library, NATIVE, isValidLib = known, enableLibraryInsight = true)
                .also { it.showNow(activity.supportFragmentManager, "firstOpen") }
            }
            val root = firstDialog?.view as? ViewGroup ?: return@onActivity
            visible = root.descendants.filterIsInstance<TextView>().any { it.text.toString() == version && it.isShown && it.getGlobalVisibleRect(Rect()) }
            if (visible) {
              assertFalse("Description spinner remained visible", root.descendants.any { it is RuleLoadingView && it.isShown })
            }
          }
          if (visible) {
            assertTrue("Missing-description path was not exercised", missingRequested.get())
            return
          }
          SystemClock.sleep(100)
        } while (SystemClock.elapsedRealtime() < deadline)
        throw AssertionError("First open of $library did not show $version (description requested: ${missingRequested.get()})")
      }
    } finally {
      loadKoinModules(
        module {
          single<LibraryDetailRepository> { originalDetails }
          originalInsights?.let { saved -> single<LibraryInsightRepository> { saved } }
        }
      )
    }
  }

  private fun candidateDefinition(arguments: Bundle): LibraryInsightDefinition {
    val encoded = arguments.getString("candidateBase64")
    assumeNotNull(encoded)
    return requireNotNull(
      JsonUtil.moshi.adapter(LibraryInsightDefinition::class.java)
        .fromJson(Base64.decode(encoded, Base64.DEFAULT).decodeToString())
    )
  }

  private fun stagedRepository(definition: LibraryInsightDefinition): LibraryInsightRepository {
    val sdk = definition.sdkId
    val path = "sdk-details/sdks/$sdk/definition.json"
    val catalog = LibraryInsightCatalog(1, listOf(LibraryInsightCatalog.Entry(sdk, definition.targetUuids, path, definition.nativeLibraryNames)))
    return object : LibraryInsightRepository {
      override suspend fun getCatalog() = RemoteDocumentResult.Success(catalog)
      override suspend fun getDefinition(path: String) = RemoteDocumentResult.Success(definition)
      override suspend fun getLookup(path: String): RemoteDocumentResult<Map<String, Any?>> = error("Unexpected lookup")
    }
  }

  /** Exercises a staged definition against an installed APK before the catalog is published. */
  @Test
  fun resolvesCandidateOnInstalledPackage() = runBlocking {
    val arguments = InstrumentationRegistry.getArguments()
    val definition = candidateDefinition(arguments)
    val repository = stagedRepository(definition)
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val info = instrumentation.targetContext.packageManager.getPackageInfo(requireNotNull(arguments.getString("targetPackage")), 0)
    val resolve = ResolveLibraryInsightUseCase(repository, LibraryInsightDefinitionValidator(), LibraryInsightProbeEngine())
    val result = resolve(definition.targetUuids.single(), info, "zh-Hans") as LibraryInsightResult.Content
    val field = result.content.summary.single()
    assertEquals(listOf(requireNotNull(arguments.getString("expectedVersion"))), field.values)
    instrumentation.sendStatus(0, Bundle().apply { putString("stream", "${field.label} ${field.values.single()}\n") })
  }

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
