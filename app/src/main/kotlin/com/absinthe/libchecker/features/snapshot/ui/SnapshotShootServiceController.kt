package com.absinthe.libchecker.features.snapshot.ui

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.absinthe.libchecker.services.IShootService
import com.absinthe.libchecker.services.OnShootListener
import com.absinthe.libchecker.services.ShootService
import timber.log.Timber

class SnapshotShootServiceController(
  private val listener: OnShootListener
) {

  private var binder: IShootService? = null
  var isStarted: Boolean = false
    private set

  val isShooting: Boolean
    get() = binder?.isShooting == true

  private val connection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
      isStarted = true
      if (binder == null && service?.pingBinder() == true) {
        binder = IShootService.Stub.asInterface(service).also {
          it.registerOnShootOverListener(listener)
        }
      }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
      isStarted = false
      binder = null
    }
  }

  fun startAndBind(context: Context) {
    runCatching {
      val appContext = context.applicationContext
      val intent = Intent(appContext, ShootService::class.java).apply {
        setPackage(appContext.packageName)
      }
      appContext.startService(intent)
      appContext.bindService(
        intent,
        connection,
        Service.BIND_AUTO_CREATE
      )
    }.onFailure { t ->
      Timber.e(t)
    }.onSuccess {
      isStarted = true
    }
  }

  fun start(context: Context) {
    runCatching {
      context.applicationContext.startService(
        Intent(context.applicationContext, ShootService::class.java)
      )
    }.onFailure {
      Timber.w(it, "Failed to start snapshot service")
    }
  }

  fun computeSnapshot(dropPrevious: Boolean): Boolean {
    val activeBinder = binder ?: run {
      Timber.w("shoot binder is null")
      return false
    }

    return runCatching {
      activeBinder.computeSnapshot(dropPrevious)
    }.onFailure {
      Timber.w(it, "Failed to compute snapshot")
    }.isSuccess
  }

  fun release(context: Context) {
    binder?.let { activeBinder ->
      runCatching {
        activeBinder.unregisterOnShootOverListener(listener)
      }.onFailure {
        Timber.w(it, "Failed to unregister snapshot listener")
      }

      if (ShootService.isComputing.not()) {
        runCatching {
          val appContext = context.applicationContext
          appContext.unbindService(connection)
          appContext.stopService(
            Intent(
              appContext,
              ShootService::class.java
            )
          )
        }.onFailure {
          Timber.w(it, "Failed to release snapshot service")
        }
      }
    }
    binder = null
    isStarted = false
  }
}
