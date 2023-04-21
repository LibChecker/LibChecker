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
import com.absinthe.libchecker.data.app.LocalAppDataSource
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getFeatures
import java.lang.ref.WeakReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import timber.log.Timber

class WorkerService : LifecycleService() {

  private var lastPackageChangedTime: Long = 0L

  private val packageReceiver by lazy {
    object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent?) {
        Timber.d("package receiver received: ${intent?.action}")
        lastPackageChangedTime = SystemClock.elapsedRealtime()
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
    initializingFeatures = false

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

  private fun initFeatures() {
    Timber.d("initFeatures")
    initializingFeatures = true

    Repositories.lcRepository.allLCItemsFlow.onEach {
      it.forEach { item ->
        if (item.features == -1) {
          runCatching {
            val feature = PackageUtils.getPackageInfo(item.packageName).getFeatures()
            Repositories.lcRepository.updateFeatures(item.packageName, feature)
          }.onFailure { e ->
            Timber.w(e)
          }
        }
      }
      initializingFeatures = false
      Timber.d("initFeatures finished")
    }
      .flowOn(Dispatchers.IO)
      .launchIn(lifecycleScope)
  }

  class WorkerBinder(service: WorkerService) : IWorkerService.Stub() {

    private val serviceRef: WeakReference<WorkerService> = WeakReference(service)

    override fun initFeatures() {
      serviceRef.get()?.initFeatures()
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
    var initializingFeatures: Boolean = false
  }
}
