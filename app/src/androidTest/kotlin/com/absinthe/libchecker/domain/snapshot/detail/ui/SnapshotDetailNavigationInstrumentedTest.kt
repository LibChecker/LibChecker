package com.absinthe.libchecker.domain.snapshot.detail.ui

import android.content.Context
import android.content.Intent
import android.os.Parcel
import android.widget.TextView
import androidx.core.view.descendants
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.absinthe.libchecker.R
import com.absinthe.libchecker.data.snapshot.FileSnapshotDetailPayloadRepository
import com.absinthe.libchecker.domain.snapshot.list.ui.SnapshotNoDiffBSDFragment
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.domain.snapshot.track.ui.TrackActivity
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SnapshotDetailNavigationInstrumentedTest {
  @Test fun largePopupArgumentsAndSavedStateStaySmallAndRecreate() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val directory = File(context.cacheDir, "snapshot_details")
    val item = snapshotDiffItem().copy(
      activitiesDiff = SnapshotDiffItem.DiffNode("old".repeat(400000), "new".repeat(400000)),
      newInstalled = true
    )
    val id = FileSnapshotDetailPayloadRepository(directory).save(item)
    try {
      val arguments = SnapshotNoDiffBSDFragment.newInstance(id).requireArguments()
      val parcel = Parcel.obtain()
      val restoredArguments = try {
        parcel.writeBundle(arguments)
        assertTrue("Popup arguments must stay below 4 KiB", parcel.dataSize() < 4096)
        parcel.setDataPosition(0)
        parcel.readBundle(SnapshotNoDiffBSDFragment::class.java.classLoader)!!
      } finally {
        parcel.recycle()
      }
      assertEquals(setOf(EXTRA_PAYLOAD_ID), restoredArguments.keySet())
      assertEquals(item, FileSnapshotDetailPayloadRepository(directory).load(restoredArguments.getString(EXTRA_PAYLOAD_ID)!!))
      val legacyParcel = Parcel.obtain()
      try {
        legacyParcel.writeSerializable(item)
        assertTrue("Fixture must exceed Binder-sized payloads", legacyParcel.dataSize() > 1000000)
      } finally {
        legacyParcel.recycle()
      }
      ActivityScenario.launch(TrackActivity::class.java).use { scenario ->
        scenario.onActivity {
          SnapshotNoDiffBSDFragment().apply { this.arguments = restoredArguments }
            .showNow(it.supportFragmentManager, "large-popup")
        }
        awaitPopup(scenario)
        scenario.onActivity {
          val fragment = it.supportFragmentManager.findFragmentByTag("large-popup")!!
          val savedState = it.supportFragmentManager.saveFragmentInstanceState(fragment)!!
          val stateParcel = Parcel.obtain()
          try {
            savedState.writeToParcel(stateParcel, 0)
            assertTrue("Popup saved state must stay below 32 KiB", stateParcel.dataSize() < 32768)
          } finally {
            stateParcel.recycle()
          }
        }
        scenario.recreate()
        awaitPopup(scenario)
      }
    } finally {
      File(directory, id).delete()
    }
  }

  @Test fun missingPopupPayloadDismissesAfterRecreation() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val directory = File(context.cacheDir, "snapshot_details")
    val id = FileSnapshotDetailPayloadRepository(directory).save(snapshotDiffItem().copy(newInstalled = true))
    try {
      ActivityScenario.launch(TrackActivity::class.java).use { scenario ->
        scenario.onActivity {
          SnapshotNoDiffBSDFragment.newInstance(id).showNow(it.supportFragmentManager, "large-popup")
        }
        awaitPopup(scenario)
        assertTrue(File(directory, id).delete())
        scenario.recreate()
        repeat(100) {
          var dismissed = false
          scenario.onActivity { dismissed = it.supportFragmentManager.findFragmentByTag("large-popup") == null }
          if (dismissed) return@use
          Thread.sleep(100)
        }
        throw AssertionError("Popup with missing payload was not dismissed")
      }
    } finally {
      File(directory, id).delete()
    }
  }

  private fun awaitPopup(scenario: ActivityScenario<TrackActivity>) {
    repeat(100) {
      var rendered = false
      scenario.onActivity { activity ->
        val popup = activity.supportFragmentManager.findFragmentByTag("large-popup") as? SnapshotNoDiffBSDFragment
        val texts = popup?.view?.let { it as android.view.ViewGroup }?.descendants
          ?.filterIsInstance<TextView>()?.filter { it.isShown }?.map { it.text.toString() }?.toList().orEmpty()
        rendered = "Example" in texts && activity.getString(R.string.snapshot_detail_new_install_title) in texts
      }
      if (rendered) return
      Thread.sleep(100)
    }
    throw AssertionError("Snapshot popup did not render its payload")
  }

  @Test fun switchingAwayCancelsPendingNavigationAndReturningDoesNotReplayIt() = runBlocking {
    withContext(Dispatchers.Main) {
      val owner = object : LifecycleOwner {
        override val lifecycle = LifecycleRegistry(this)
      }
      owner.lifecycle.currentState = Lifecycle.State.RESUMED
      val saving = CompletableDeferred<Unit>()
      val saved = CompletableDeferred<Unit>()
      var launches = 0
      val pending = launch {
        whileSnapshotNavigationResumed(owner.lifecycle) {
          saving.complete(Unit)
          saved.await()
          launches++
        }
      }
      saving.await()
      owner.lifecycle.currentState = Lifecycle.State.STARTED
      owner.lifecycle.currentState = Lifecycle.State.RESUMED
      saved.complete(Unit)
      pending.join()
      assertTrue(pending.isCancelled)
      assertEquals(0, launches)
      whileSnapshotNavigationResumed(owner.lifecycle) { launches++ }
      assertEquals(1, launches)
      owner.lifecycle.currentState = Lifecycle.State.DESTROYED
    }
  }

  @Test fun inactivePageDoesNotStartSavingOrNavigating() = runBlocking {
    withContext(Dispatchers.Main) {
      val owner = object : LifecycleOwner {
        override val lifecycle = LifecycleRegistry(this)
      }
      owner.lifecycle.currentState = Lifecycle.State.STARTED
      var started = false
      whileSnapshotNavigationResumed(owner.lifecycle) { started = true }
      assertFalse(started)
      owner.lifecycle.currentState = Lifecycle.State.DESTROYED
    }
  }

  @Test fun largeComparisonActivityCanBeRecreated() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val directory = File(context.cacheDir, "snapshot_details")
    val components = (1..30000).joinToString(prefix = "[", postfix = "]") { "\"com.example.Activity$it\"" }
    val item = snapshotDiffItem().copy(
      activitiesDiff = SnapshotDiffItem.DiffNode(components, components)
    )
    val id = FileSnapshotDetailPayloadRepository(directory).save(item)
    try {
      ActivityScenario.launch<SnapshotDetailActivity>(snapshotDetailIntent(context, id)).use { scenario ->
        awaitPayload(scenario)
        scenario.recreate()
        awaitPayload(scenario)
      }
    } finally {
      File(directory, id).delete()
    }
  }

  private fun awaitPayload(scenario: ActivityScenario<SnapshotDetailActivity>) {
    repeat(100) {
      var packageName: String? = null
      scenario.onActivity { packageName = it.requirePackageName() }
      if (packageName == "com.example") return
      Thread.sleep(100)
    }
    throw AssertionError("Snapshot detail did not load its payload")
  }

  @Test fun multiMegabyteComparisonUsesSmallParcelAndRestoresWithoutMemoryCache() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val directory = File(context.cacheDir, "navigation-test-${UUID.randomUUID()}")
    try {
      val item = snapshotDiffItem().copy(
        activitiesDiff = SnapshotDiffItem.DiffNode("old".repeat(400000), "new".repeat(400000))
      )
      val id = FileSnapshotDetailPayloadRepository(directory).save(item)
      val intent = snapshotDetailIntent(context, id)
      val parcel = Parcel.obtain()
      try {
        intent.writeToParcel(parcel, 0)
        assertTrue("Intent must stay below 4 KiB", parcel.dataSize() < 4096)
        parcel.setDataPosition(0)
        val restored = Intent.CREATOR.createFromParcel(parcel)
        assertEquals(setOf(EXTRA_PAYLOAD_ID), restored.extras!!.keySet())
        assertEquals(item, FileSnapshotDetailPayloadRepository(directory).load(restored.getStringExtra(EXTRA_PAYLOAD_ID)!!))
      } finally {
        parcel.recycle()
      }
      val legacyParcel = Parcel.obtain()
      try {
        Intent().putExtra("EXTRA_ENTITY", item).writeToParcel(legacyParcel, 0)
        assertTrue("Fixture must exceed Binder-sized payloads", legacyParcel.dataSize() > 1000000)
      } finally {
        legacyParcel.recycle()
      }
    } finally {
      directory.deleteRecursively()
    }
  }

  private fun snapshotDiffItem(dexInfo: String = "[]"): SnapshotDiffItem {
    return SnapshotDiffItem(
      packageName = "com.example",
      updateTime = 1,
      labelDiff = SnapshotDiffItem.DiffNode("Example", "Example"),
      versionNameDiff = SnapshotDiffItem.DiffNode("1", "1"),
      versionCodeDiff = SnapshotDiffItem.DiffNode(1, 1),
      abiDiff = SnapshotDiffItem.DiffNode(0, 0),
      targetApiDiff = SnapshotDiffItem.DiffNode(35, 35),
      compileSdkDiff = SnapshotDiffItem.DiffNode(35, 35),
      minSdkDiff = SnapshotDiffItem.DiffNode(24, 24),
      nativeLibsDiff = SnapshotDiffItem.DiffNode("[]", "[]"),
      servicesDiff = SnapshotDiffItem.DiffNode("[]", "[]"),
      activitiesDiff = SnapshotDiffItem.DiffNode("[]", "[]"),
      receiversDiff = SnapshotDiffItem.DiffNode("[]", "[]"),
      providersDiff = SnapshotDiffItem.DiffNode("[]", "[]"),
      permissionsDiff = SnapshotDiffItem.DiffNode("[]", "[]"),
      metadataDiff = SnapshotDiffItem.DiffNode("[]", "[]"),
      packageSizeDiff = SnapshotDiffItem.DiffNode(1, 1),
      dexInfoDiff = SnapshotDiffItem.DiffNode(dexInfo, dexInfo),
      resourcesSizeDiff = SnapshotDiffItem.DiffNode(1, 1),
      changed = 1
    )
  }
}
