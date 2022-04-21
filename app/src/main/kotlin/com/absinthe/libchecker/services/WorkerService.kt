package com.absinthe.libchecker.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.os.SystemClock
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.app.Global
import com.absinthe.libchecker.database.AppItemRepository
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.utils.PackageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.ref.WeakReference

class WorkerService : LifecycleService() {

  private var lastPackageChangedTime: Long = 0L

  private val packageReceiver by lazy {
    object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent?) {
        Timber.d("package receiver received: ${intent?.action}")
        lastPackageChangedTime = SystemClock.elapsedRealtime()
        initAllApplicationInfoItems()
        notifyPackagesChanged(
          intent?.data?.encodedSchemeSpecificPart.orEmpty(),
          intent?.action.orEmpty()
        )
      }
    }
  }

  private val listenerList = RemoteCallbackList<OnWorkerListener>()
  private val binder by lazy { WorkerBinder(this) }

  override fun onBind(intent: Intent): IBinder {
    super.onBind(intent)
    return binder
  }

  override fun onCreate() {
    super.onCreate()
    Timber.d("onCreate")
    initAllApplicationInfoItems()

    val intentFilter = IntentFilter().apply {
      addAction(Intent.ACTION_PACKAGE_ADDED)
      addAction(Intent.ACTION_PACKAGE_REPLACED)
      addAction(Intent.ACTION_PACKAGE_REMOVED)
      addDataScheme("package")
    }
    registerReceiver(packageReceiver, intentFilter)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.`package` != packageName) {
      stopSelf()
    }
    return super.onStartCommand(intent, flags, startId)
  }

  override fun onDestroy() {
    Timber.d("onDestroy")
    unregisterReceiver(packageReceiver)
    super.onDestroy()
  }

  private fun initAllApplicationInfoItems() {
    Global.applicationListJob?.cancel()
    Global.applicationListJob = lifecycleScope.launch(Dispatchers.IO) {
      AppItemRepository.allPackageInfoMap = PackageUtils.getAppsList().asSequence()
        .map { it.packageName to it }
        .toMap()
      Global.applicationListJob = null
    }.also {
      it.start()
    }
  }

  @Synchronized
  private fun notifyPackagesChanged(packageName: String, action: String) {
    val count = listenerList.beginBroadcast()
    for (i in 0 until count) {
      try {
        listenerList.getBroadcastItem(i).onReceivePackagesChanged(packageName, action)
      } catch (e: RemoteException) {
        Timber.e(e)
      }
    }
    listenerList.finishBroadcast()
  }

  private fun initKotlinUsage() {
    val map = mutableMapOf<String, Boolean>()
    var count = 0

    initializingKotlinUsage = true

    lifecycleScope.launch(Dispatchers.IO) {
      while (Repositories.lcRepository.allDatabaseItems.value == null) {
        delay(300)
      }

      Repositories.lcRepository.allDatabaseItems.value!!.forEach { lcItem ->
        if (lcItem.isKotlinUsed == null) {
          runCatching {
            map[lcItem.packageName] = PackageUtils.isKotlinUsed(
              PackageUtils.getPackageInfo(lcItem.packageName)
            )
          }.onFailure {
            Timber.w(it)
          }
          count++
        }

        if (count == 20) {
          Repositories.lcRepository.updateKotlinUsage(map)
          map.clear()
          count = 0
        }
      }

      if (count > 0) {
        Repositories.lcRepository.updateKotlinUsage(map)
        map.clear()
        count = 0
      }

      initializingKotlinUsage = false
    }
  }

  class WorkerBinder(service: WorkerService) : IWorkerService.Stub() {

    private val serviceRef: WeakReference<WorkerService> = WeakReference(service)

    override fun initKotlinUsage() {
      Timber.d("initKotlinUsage")
      serviceRef.get()?.initKotlinUsage()
    }

    override fun getLastPackageChangedTime(): Long {
      return serviceRef.get()?.lastPackageChangedTime ?: 0
    }

    override fun registerOnWorkerListener(listener: OnWorkerListener?) {
      Timber.d("registerOnWorkerListener")
      listener?.let {
        serviceRef.get()?.listenerList?.register(listener)
      }
    }

    override fun unregisterOnWorkerListener(listener: OnWorkerListener?) {
      Timber.d("unregisterOnWorkerListener")
      serviceRef.get()?.listenerList?.unregister(listener)
    }
  }

  companion object {
    var initializingKotlinUsage: Boolean = false
  }
}
