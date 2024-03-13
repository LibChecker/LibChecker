package com.absinthe.libchecker.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.os.SystemClock
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getFeatures
import java.lang.ref.WeakReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

class WorkerService : LifecycleService() {

  private lateinit var mainHandler: MyHandler
  private var lastPackageChangedTime: Long = 0L

  private val packageReceiver by lazy {
    object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent?) {
        Timber.d("package receiver received: ${intent?.action}")

        val what = intent?.data?.encodedSchemeSpecificPart.orEmpty().hashCode()
        mainHandler.removeMessages(what)
        mainHandler.sendMessageDelayed(Message.obtain(mainHandler, what, intent), 1000)
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
    mainHandler = MyHandler(WeakReference(this))

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
    mainHandler.removeCallbacksAndMessages(null)
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
            val feature = PackageUtils.getPackageInfo(item.packageName, PackageManager.GET_META_DATA).getFeatures()
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

  private class MyHandler(private val serviceRef: WeakReference<WorkerService>) : Handler(Looper.getMainLooper()) {
    override fun handleMessage(msg: Message) {
      super.handleMessage(msg)
      val service = serviceRef.get()
      if (service != null && msg.obj is Intent) {
        val intent = msg.obj as Intent
        Timber.d("handleMessage: $intent")
        service.lastPackageChangedTime = SystemClock.elapsedRealtime()
        service.notifyPackagesChanged(
          intent.data?.encodedSchemeSpecificPart.orEmpty(),
          intent.action.orEmpty()
        )
      }
    }
  }

  companion object {
    var initializingFeatures: Boolean = false
  }
}
