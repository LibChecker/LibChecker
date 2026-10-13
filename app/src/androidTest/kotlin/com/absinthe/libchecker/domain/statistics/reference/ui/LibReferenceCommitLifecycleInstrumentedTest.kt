package com.absinthe.libchecker.domain.statistics.reference.ui

import android.os.SystemClock
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.viewpager2.widget.ViewPager2
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.domain.home.ui.MainActivity
import com.absinthe.libchecker.domain.statistics.reference.model.LibReference
import com.absinthe.libchecker.domain.statistics.reference.presentation.LibReferenceViewModel
import com.absinthe.libchecker.domain.statistics.reference.ui.adapter.LibReferenceAdapter
import com.google.android.material.navigation.NavigationBarView
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibReferenceCommitLifecycleInstrumentedTest {
  @Test
  fun tabChangeSkipsSnapshotWhoseViewWasDestroyed() {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      scenario.onActivity {
        it.findViewById<NavigationBarView>(R.id.nav_view).selectedItemId = R.id.navigation_snapshot
      }
      val deadline = SystemClock.uptimeMillis() + 30_000
      var ready = false
      while (!ready && SystemClock.uptimeMillis() < deadline) {
        SystemClock.sleep(100)
        scenario.onActivity {
          ready = it.supportFragmentManager.findFragmentByTag("f2")?.view != null
        }
      }
      assertTrue("Snapshot page must have a view", ready)
      var failure: Throwable? = null
      scenario.onActivity {
        val fragment = checkNotNull(it.supportFragmentManager.findFragmentByTag("f2"))
        it.supportFragmentManager.beginTransaction().detach(fragment).commitNow()
        assertNull(fragment.view)
        failure = runCatching {
          it.findViewById<ViewPager2>(R.id.viewpager).setCurrentItem(0, false)
        }.exceptionOrNull()
      }
      assertNull("Page visibility must not access a destroyed view: $failure", failure)
    }
  }

  @Test
  fun pendingDiffDoesNotAccessDestroyedView() {
    val commits = LinkedBlockingQueue<Runnable>()
    lateinit var fragment: LibReferenceFragment
    lateinit var adapter: LibReferenceAdapter
    lateinit var model: LibReferenceViewModel
    val updatedReferences = listOf(reference("new"))
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      scenario.onActivity {
        it.findViewById<NavigationBarView>(R.id.nav_view).selectedItemId = R.id.navigation_classify
        it.supportFragmentManager.executePendingTransactions()
      }
      val deadline = SystemClock.uptimeMillis() + 30_000
      var ready = false
      while (!ready && SystemClock.uptimeMillis() < deadline) {
        SystemClock.sleep(100)
        scenario.onActivity {
          ready = findReference(it.supportFragmentManager)?.view != null
        }
      }
      scenario.onActivity {
        fragment = checkNotNull(findReference(it.supportFragmentManager))
        model = (field(fragment, "libReferenceViewModel\$delegate") as Lazy<*>).value as LibReferenceViewModel
        val controller = field(model, "libReferenceComputationController")!!
        (field(controller, "computationJob") as? Job)?.cancel()
        adapter = (field(fragment, "refAdapter\$delegate") as Lazy<*>).value as LibReferenceAdapter
        adapter.setList(emptyList())
        adapter.setList(listOf(reference("old")))
        val differ = field(adapter, "mDiffer") as AsyncListDiffer<*>
        differ.javaClass.getDeclaredField("mMainThreadExecutor").apply {
          isAccessible = true
          set(differ, Executor { commits.add(it) })
        }
        @Suppress("UNCHECKED_CAST")
        val references = field(controller, "_libReference") as MutableStateFlow<List<LibReference>?>
        references.value = updatedReferences
      }
      val submissionDeadline = SystemClock.uptimeMillis() + 10_000
      while ((model.libReference.value != updatedReferences || commits.isEmpty()) &&
        SystemClock.uptimeMillis() < submissionDeadline
      ) {
        SystemClock.sleep(10)
      }
      var failure: Throwable? = null
      scenario.onActivity {
        fragment.parentFragmentManager.beginTransaction().remove(fragment).commitNow()
        assertFalse(fragment.isBindingInitialized())
        assertFalse(fragment.isDetached)
        (field(fragment, "prewarmNextViewHolder") as Runnable).run()
      }
      val commitDeadline = SystemClock.uptimeMillis() + 10_000
      var applied = false
      while (!applied && failure == null && SystemClock.uptimeMillis() < commitDeadline) {
        val commit = commits.poll(1, TimeUnit.SECONDS) ?: continue
        scenario.onActivity {
          failure = runCatching { commit.run() }.exceptionOrNull()
          applied = adapter.data == updatedReferences
        }
      }
      assertEquals("The new diff must actually apply, rather than be discarded as stale", updatedReferences, adapter.data)
      assertNull("A completed diff must not render a destroyed view: $failure", failure)
    }
  }

  private fun reference(name: String) = LibReference(name, null, setOf("com.tencent.mm"), NATIVE)

  private fun findReference(manager: FragmentManager): LibReferenceFragment? {
    manager.fragments.forEach {
      if (it is LibReferenceFragment) return it
      findReference(it.childFragmentManager)?.let { reference -> return reference }
    }
    return null
  }

  private fun field(instance: Any, name: String): Any? {
    var type: Class<*>? = instance.javaClass
    while (type != null) {
      val field = type.declaredFields.firstOrNull { it.name == name }
      if (field != null) return field.apply { isAccessible = true }.get(instance)
      type = type.superclass
    }
    error("Missing field $name")
  }
}
