package com.absinthe.libchecker.domain.settings.ui

import android.view.View
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.core.view.descendants
import androidx.fragment.app.Fragment
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.rules.CloudRulesRepository
import com.absinthe.libchecker.domain.rules.CloudRulesVersionInfo
import com.absinthe.libchecker.domain.snapshot.track.ui.TrackActivity
import com.absinthe.libchecker.view.app.RuleLoadingView
import com.google.android.material.button.MaterialButton
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class CloudRulesDialogInstrumentedTest {
  @Test
  fun retryUsesOnlyOneFadeAnimation() = withRepository { repository ->
    ActivityScenario.launch(TrackActivity::class.java).use { scenario ->
      val dialog = scenario.showCloudRules()
      repository.nextRequest().complete(null)
      scenario.await { dialog.hasRetryActionButton() && !dialog.isLoadingShown() }
      lateinit var outgoing: View
      scenario.onActivity {
        outgoing = dialog.retryActionButton().parent as View
        val flipper = outgoing.parent as ViewFlipper
        assertNull("The built-in fade must not overlap an XML exit animation", flipper.outAnimation)
        assertNull("The built-in fade must not overlap an XML entry animation", flipper.inAnimation)
        dialog.retryActionButton().performClick()
      }
      repository.nextRequest()
      scenario.await { dialog.isLoadingShown() }
      scenario.onActivity {
        assertTrue(dialog.isLoadingShown())
        assertNull("The built-in property fade must not overlap a legacy alpha animation", outgoing.animation)
      }
      scenario.await { !dialog.hasRetryActionButton() }
      scenario.onActivity { dialog.dismissNow() }
    }
  }

  @Test
  fun failedAndEmptyResponsesCanRetryAndRecover() = withRepository { repository ->
    ActivityScenario.launch(TrackActivity::class.java).use { scenario ->
      val dialog = scenario.showCloudRules()
      repository.nextRequest().completeExceptionally(IOException("Offline"))
      scenario.await { dialog.hasRetryActionButton() && !dialog.isLoadingShown() }
      scenario.onActivity {
        assertFalse(dialog.root.descendants.filterIsInstance<RuleLoadingView>().single().isShown)
        assertTrue(
          dialog.root.descendants.filterIsInstance<TextView>().any { text ->
            text.isShown && text.text.toString() == it.getString(R.string.toast_cloud_rules_update_error)
          }
        )
        val visibleText = dialog.root.descendants.filterIsInstance<TextView>().filter { it.isShown }.map { it.text.toString() }.toList()
        assertTrue(visibleText.containsAll(listOf("48", "?")))
        dialog.retryActionButton().performClick()
      }

      val retry = repository.nextRequest()
      scenario.await { dialog.isLoadingShown() && !dialog.hasRetryActionButton() }
      retry.complete(null)
      scenario.await { dialog.hasRetryActionButton() && !dialog.isLoadingShown() }
      scenario.onActivity { dialog.retryActionButton().performClick() }
      repository.nextRequest().complete(CloudRulesVersionInfo(48, 49))
      scenario.await {
        !dialog.hasRetryActionButton() && !dialog.isLoadingShown() && dialog.root.descendants.filterIsInstance<MaterialButton>().any { button ->
          button.isShown && button.isEnabled && button.text.toString() == it.getString(R.string.rules_btn_restart_to_update)
        }
      }
      scenario.onActivity {
        assertFalse(dialog.hasRetryActionButton())
        assertFalse(dialog.root.descendants.filterIsInstance<RuleLoadingView>().single().isShown)
        val visibleText = dialog.root.descendants.filterIsInstance<TextView>().filter { it.isShown }.map { it.text.toString() }.toList()
        assertTrue(visibleText.containsAll(listOf("48", "49")))
        dialog.dismissNow()
      }
      assertEquals(3, repository.calls.get())
    }
  }

  @Test
  fun dismissCancelsPendingVersionRequest() = withRepository { repository ->
    ActivityScenario.launch(TrackActivity::class.java).use { scenario ->
      val dialog = scenario.showCloudRules()
      repository.nextRequest()
      scenario.onActivity { dialog.dismissNow() }
      scenario.await { repository.cancellations.get() == 1 }
      assertEquals(1, repository.calls.get())
    }
  }

  private fun ActivityScenario<TrackActivity>.showCloudRules(): CloudRulesDialogFragment {
    val dialog = CloudRulesDialogFragment()
    onActivity { activity ->
      val parent = Fragment()
      activity.supportFragmentManager.beginTransaction().add(parent, "cloud-rules-test").commitNow()
      dialog.showNow(parent.childFragmentManager, "cloud-rules")
    }
    return dialog
  }

  private fun CloudRulesDialogFragment.retryActionButton(): MaterialButton = root.descendants.filterIsInstance<MaterialButton>().single {
    it.text.toString() == getString(R.string.toast_cloud_rules_update_error)
  }

  private fun CloudRulesDialogFragment.hasRetryActionButton(): Boolean = root.descendants.filterIsInstance<MaterialButton>().any {
    it.isShown && it.text.toString() == getString(R.string.toast_cloud_rules_update_error)
  }

  private fun CloudRulesDialogFragment.isLoadingShown(): Boolean = root.descendants.filterIsInstance<RuleLoadingView>().single().isShown

  private fun ActivityScenario<TrackActivity>.await(condition: (TrackActivity) -> Boolean) {
    val deadline = System.nanoTime() + 5_000_000_000L
    while (System.nanoTime() < deadline) {
      var matched = false
      onActivity { matched = condition(it) }
      if (matched) return
      Thread.sleep(25)
    }
    throw AssertionError("Cloud rules UI did not reach the expected state")
  }

  private fun withRepository(block: (PendingCloudRulesRepository) -> Unit) {
    val koin = GlobalContext.get()
    val original = koin.get<CloudRulesRepository>()
    val repository = PendingCloudRulesRepository(original)
    koin.declare<CloudRulesRepository>(repository)
    try {
      block(repository)
    } finally {
      InstrumentationRegistry.getInstrumentation().waitForIdleSync()
      koin.declare<CloudRulesRepository>(original)
    }
  }

  private class PendingCloudRulesRepository(original: CloudRulesRepository) : CloudRulesRepository by original {
    val calls = AtomicInteger()
    val cancellations = AtomicInteger()
    private val requests = Channel<CompletableDeferred<CloudRulesVersionInfo?>>(Channel.UNLIMITED)

    override suspend fun getLocalVersion(): Int = 48

    override suspend fun getVersionInfo(): CloudRulesVersionInfo? {
      calls.incrementAndGet()
      val result = CompletableDeferred<CloudRulesVersionInfo?>()
      requests.send(result)
      return try {
        result.await()
      } catch (e: CancellationException) {
        cancellations.incrementAndGet()
        throw e
      }
    }

    fun nextRequest(): CompletableDeferred<CloudRulesVersionInfo?> = runBlocking {
      withTimeout(5_000) { requests.receive() }
    }
  }
}
