package com.absinthe.libchecker.utils

import android.content.pm.IPackageManager
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Parcel
import android.os.ParcelFileDescriptor
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.constant.Constants
import java.io.File
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import rikka.sui.Sui
import timber.log.Timber

object ShizukuManager {

  private const val MIN_VERSION = 10
  private const val PACKAGE_SERVICE = "package"

  enum class Availability {
    Available,
    NotInstalled,
    NotRunning,
    LowVersion,
    PermissionDenied
  }

  class ListenerHandle internal constructor(
    private val unregister: () -> Unit
  ) {
    private var closed = false

    fun close() {
      if (closed) {
        return
      }
      closed = true
      unregister()
    }
  }

  fun getAvailability(): Availability {
    return runCatching {
      when {
        !Shizuku.pingBinder() -> Availability.NotRunning
        Shizuku.getVersion() < MIN_VERSION -> Availability.LowVersion
        Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED -> Availability.PermissionDenied
        else -> Availability.Available
      }
    }.onFailure {
      Timber.w(it, "Shizuku availability check failed")
    }.getOrDefault(Availability.NotRunning)
  }

  fun requireAvailable(): Boolean {
    return getAvailability() == Availability.Available
  }

  fun requestPermission(requestCode: Int) {
    Shizuku.requestPermission(requestCode)
  }

  fun getUid(): Int {
    return Shizuku.getUid()
  }

  fun registerBinderReceivedListener(onBinderReceived: () -> Unit): ListenerHandle {
    val listener = Shizuku.OnBinderReceivedListener {
      onBinderReceived()
    }
    Shizuku.addBinderReceivedListener(listener)
    return ListenerHandle {
      Shizuku.removeBinderReceivedListener(listener)
    }
  }

  fun registerRequestPermissionResultListener(
    onRequestPermissionResult: (requestCode: Int, grantResult: Int) -> Unit
  ): ListenerHandle {
    val listener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
      onRequestPermissionResult(requestCode, grantResult)
    }
    Shizuku.addRequestPermissionResultListener(listener)
    return ListenerHandle {
      Shizuku.removeRequestPermissionResultListener(listener)
    }
  }

  fun getPackageManager(): IPackageManager {
    return IPackageManager.Stub.asInterface(getSystemServiceBinder(PACKAGE_SERVICE))
  }

  fun dumpPackageService(args: Array<String>): String {
    return dumpSystemService(PACKAGE_SERVICE, args)
  }

  private fun dumpSystemService(serviceName: String, args: Array<String>): String {
    val dumpFile = File.createTempFile("$serviceName-", ".dump", LibCheckerApp.app.cacheDir)
    return try {
      ParcelFileDescriptor.open(
        dumpFile,
        ParcelFileDescriptor.MODE_CREATE or
          ParcelFileDescriptor.MODE_TRUNCATE or
          ParcelFileDescriptor.MODE_READ_WRITE
      ).use { pfd ->
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
          data.writeFileDescriptor(pfd.fileDescriptor)
          data.writeStringArray(args)
          getSystemServiceBinder(serviceName).transact(
            IBinder.DUMP_TRANSACTION,
            data,
            reply,
            0
          )
          reply.readException()
        } finally {
          data.recycle()
          reply.recycle()
        }
      }
      dumpFile.readText()
    } finally {
      dumpFile.delete()
    }
  }

  private fun getSystemServiceBinder(name: String): IBinder {
    val binder = requireNotNull(SystemServiceHelper.getSystemService(name)) {
      "System service not found: $name"
    }
    return ShizukuBinderWrapper(binder)
  }

  private fun isSui(): Boolean {
    return runCatching {
      Sui.isSui()
    }.getOrDefault(false)
  }
}
