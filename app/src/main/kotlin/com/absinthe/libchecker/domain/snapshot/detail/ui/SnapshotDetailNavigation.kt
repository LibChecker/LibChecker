package com.absinthe.libchecker.domain.snapshot.detail.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.detail.repository.SnapshotDetailPayloadRepository
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.utils.Toasty
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import org.koin.android.ext.android.getKoin
import timber.log.Timber

internal const val EXTRA_PAYLOAD_ID = "SNAPSHOT_DETAIL_PAYLOAD_ID"

internal fun snapshotDetailIntent(context: Context, id: String, icon: Bitmap? = null): Intent {
  return Intent(context, SnapshotDetailActivity::class.java)
    .putExtra(EXTRA_PAYLOAD_ID, id)
    .apply { if (icon != null) putExtra(EXTRA_ICON, icon) }
}

suspend fun Context.launchSnapshotDetail(owner: LifecycleOwner, item: SnapshotDiffItem, icon: Bitmap? = null) {
  try {
    whileSnapshotNavigationResumed(owner.lifecycle) {
      val id = (applicationContext as Application).getKoin().get<SnapshotDetailPayloadRepository>().save(item)
      startActivity(snapshotDetailIntent(this, id, icon))
    }
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    Timber.e(e, "Failed to open snapshot detail")
    Toasty.showLong(this, R.string.toast_cant_open_app)
  }
}

internal suspend fun whileSnapshotNavigationResumed(lifecycle: Lifecycle, block: suspend () -> Unit) {
  coroutineScope {
    if (lifecycle.currentState != Lifecycle.State.RESUMED) return@coroutineScope
    val observer = LifecycleEventObserver { _, _ ->
      if (lifecycle.currentState != Lifecycle.State.RESUMED) cancel()
    }
    lifecycle.addObserver(observer)
    try {
      ensureActive()
      block()
    } finally {
      lifecycle.removeObserver(observer)
    }
  }
}
