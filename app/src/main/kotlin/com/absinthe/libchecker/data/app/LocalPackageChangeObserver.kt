package com.absinthe.libchecker.data.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.domain.app.model.PackageChangeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

class LocalPackageChangeObserver {

  private val _packageChanges: MutableSharedFlow<PackageChangeState> = MutableSharedFlow()
  val packageChanges: SharedFlow<PackageChangeState> = _packageChanges.asSharedFlow()

  private var coroutineScope: CoroutineScope? = null
  private var onPackageChanged: ((PackageChangeState) -> Unit)? = null
  private val pendingIntents = ArrayDeque<PendingPackageIntent>()

  private val packageReceiver by lazy {
    object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent?) {
        val intent = intent ?: return
        Timber.d("package receiver received: ${intent.action}, data: ${intent.data}")

        val packageName = intent.data?.encodedSchemeSpecificPart.orEmpty()
        val action = intent.action ?: return
        if (packageName.isBlank()) {
          return
        }

        pendingIntents.add(PendingPackageIntent(action, packageName))

        coroutineScope?.launch {
          delay(1000)

          while (pendingIntents.isNotEmpty() && isActive) {
            val currentIntent = pendingIntents.removeFirst()
            val currentPackageName = currentIntent.packageName

            if (pendingIntents.none { it.packageName == currentPackageName }) {
              generatePackageChangeState(currentIntent)?.let { state ->
                onPackageChanged?.invoke(state)
                _packageChanges.emit(state)
              }
            }
          }
        }
      }
    }
  }

  fun start(
    owner: LifecycleOwner,
    onPackageChanged: (PackageChangeState) -> Unit
  ) {
    coroutineScope = owner.lifecycleScope
    this.onPackageChanged = onPackageChanged

    runCatching {
      (owner as? Context)?.let {
        val intentFilter = IntentFilter().apply {
          addAction(Intent.ACTION_PACKAGE_ADDED)
          addAction(Intent.ACTION_PACKAGE_REPLACED)
          addAction(Intent.ACTION_PACKAGE_REMOVED)
          addDataScheme("package")
        }
        ContextCompat.registerReceiver(
          it,
          packageReceiver,
          intentFilter,
          ContextCompat.RECEIVER_NOT_EXPORTED
        )
      }
    }
  }

  fun stop(owner: LifecycleOwner) {
    coroutineScope = null
    onPackageChanged = null
    runCatching {
      (owner as? Context)?.unregisterReceiver(packageReceiver)
    }
  }

  private fun generatePackageChangeState(intent: PendingPackageIntent): PackageChangeState? {
    return when (intent.action) {
      Intent.ACTION_PACKAGE_ADDED -> PackageChangeState.Added(intent.packageName)
      Intent.ACTION_PACKAGE_REMOVED -> PackageChangeState.Removed(intent.packageName)
      Intent.ACTION_PACKAGE_REPLACED -> PackageChangeState.Replaced(intent.packageName)
      else -> null
    }
  }

  private data class PendingPackageIntent(
    val action: String,
    val packageName: String
  )
}
