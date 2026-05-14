package com.absinthe.libchecker.services

import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.RemoteCallbackList
import android.os.RemoteException
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.data.app.LocalAppDataSource
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getFeatures
import java.lang.ref.WeakReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class WorkerService : LifecycleService() {

  private val listenerList = RemoteCallbackList<OnWorkerListener>()
  private val binder by lazy { WorkerBinder(this) }
  private var initFeaturesJob: Job? = null

  override fun onBind(intent: Intent): IBinder {
    super.onBind(intent)
    return binder
  }

  override fun onCreate() {
    super.onCreate()
    Timber.d("onCreate")
    initializingFeatures = false
    LocalAppDataSource.addLifecycleOwner(this)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.`package` != packageName) {
      stopSelf()
    }
    return super.onStartCommand(intent, flags, startId)
  }

  override fun onDestroy() {
    Timber.d("onDestroy")
    LocalAppDataSource.removeLifecycleOwner(this)
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

  @Synchronized
  private fun initFeatures() {
    if (initFeaturesJob?.isActive == true) {
      Timber.d("initFeatures skipped")
      return
    }

    Timber.d("initFeatures")
    initializingFeatures = true

    initFeaturesJob = lifecycleScope.launch(Dispatchers.IO) {
      try {
        val packageInfoMap = LocalAppDataSource.getApplicationMap()
        val featuresMap = Repositories.lcRepository.getLCItems()
          .asSequence()
          .filter { it.features == -1 }
          .mapNotNull { item ->
            runCatching {
              val packageInfo = packageInfoMap[item.packageName]
                ?: PackageUtils.getPackageInfo(item.packageName, PackageManager.GET_META_DATA)
              item.packageName to packageInfo.getFeatures()
            }.onFailure { e ->
              Timber.w(e)
            }.getOrNull()
          }
          .toMap()

        if (featuresMap.isNotEmpty()) {
          Repositories.lcRepository.updateFeatures(featuresMap)
        }
      } finally {
        initializingFeatures = false
        Timber.d("initFeatures finished")
      }
    }
  }

  class WorkerBinder(service: WorkerService) : IWorkerService.Stub() {

    private val serviceRef: WeakReference<WorkerService> = WeakReference(service)

    override fun initFeatures() {
      serviceRef.get()?.initFeatures()
    }

    override fun getLastPackageChangedTime(): Long {
      return 0
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
