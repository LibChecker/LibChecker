package com.absinthe.libchecker.services

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageInfo
import android.os.IBinder
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.domain.app.repository.PackageListLoadException
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class ShootServiceFailureInstrumentedTest {

  @Test
  @Suppress("DEPRECATION")
  fun failedPackageScanNotifiesListenerAndClearsShootingState() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val context = instrumentation.targetContext
    // A reused service may already hold the real repository through lazy injection.
    assertFalse(
      "Close any existing snapshot screen before running this test",
      context.getSystemService(ActivityManager::class.java).getRunningServices(Int.MAX_VALUE)
        .any { it.service.className == ShootService::class.java.name }
    )
    assertFalse(ShootService.isComputing)

    val koin = GlobalContext.get()
    val original = koin.get<InstalledAppRepository>()
    val enteredScan = CountDownLatch(1)
    val releaseScan = CountDownLatch(1)
    val repository = object : InstalledAppRepository by original {
      override fun getApplicationList(forceUpdate: Boolean): List<PackageInfo> {
        enteredScan.countDown()
        releaseScan.await(10, TimeUnit.SECONDS)
        throw PackageListLoadException(IOException("Injected package scan failure"))
      }
    }
    val connected = CountDownLatch(1)
    val completed = CountDownLatch(1)
    val failed = AtomicBoolean(false)
    var service: IShootService? = null
    var bound = false
    val listener = object : OnShootListener.Stub() {
      override fun onShootFinished(timestamp: Long) {
        completed.countDown()
      }

      override fun onShootFailed() {
        failed.set(true)
        completed.countDown()
      }

      override fun onProgressUpdated(progress: Int) = Unit
    }
    val connection = object : ServiceConnection {
      override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        service = IShootService.Stub.asInterface(binder)
        connected.countDown()
      }

      override fun onServiceDisconnected(name: ComponentName) = Unit
    }

    koin.declare<InstalledAppRepository>(repository)
    try {
      instrumentation.runOnMainSync {
        bound = context.bindService(Intent(context, ShootService::class.java), connection, Context.BIND_AUTO_CREATE)
      }
      assertTrue("Service binding failed", bound)
      assertTrue("Service did not connect", connected.await(10, TimeUnit.SECONDS))
      val binder = requireNotNull(service)
      instrumentation.runOnMainSync {
        binder.registerOnShootOverListener(listener)
        binder.computeSnapshot(false)
      }
      assertTrue("Injected repository was not called", enteredScan.await(10, TimeUnit.SECONDS))
      assertTrue(binder.isShooting)
      assertTrue(ShootService.isComputing)
      releaseScan.countDown()
      assertTrue("Service did not report completion", completed.await(10, TimeUnit.SECONDS))
      assertTrue("Expected onShootFailed instead of onShootFinished", failed.get())
      assertFalse(binder.isShooting)
      assertFalse(ShootService.isComputing)
    } finally {
      releaseScan.countDown()
      try {
        instrumentation.runOnMainSync {
          try {
            service?.unregisterOnShootOverListener(listener)
          } finally {
            if (bound) context.unbindService(connection)
          }
        }
        instrumentation.waitForIdleSync()
      } finally {
        koin.declare<InstalledAppRepository>(original)
      }
    }
  }
}
