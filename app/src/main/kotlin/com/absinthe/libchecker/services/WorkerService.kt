package com.absinthe.libchecker.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.os.IBinder
import android.os.RemoteCallbackList
import android.os.RemoteException
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.app.Global
import com.absinthe.libchecker.database.AppItemRepository
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.viewmodel.GET_INSTALL_APPS_RETRY_PERIOD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class WorkerService : LifecycleService() {

  private val runtime by lazy { Runtime.getRuntime() }
  private val packageReceiver by lazy {
    object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent?) {
        Timber.d("package receiver received: ${intent?.action}")
        initAllApplicationInfoItems()
        notifyPackagesChanged(
          intent?.data?.encodedSchemeSpecificPart.orEmpty(),
          intent?.action.orEmpty()
        )
      }
    }
  }

  private val listenerList = RemoteCallbackList<OnWorkerListener>()
  private val binder = object : IWorkerService.Stub() {
    override fun registerOnWorkerListener(listener: OnWorkerListener?) {
      Timber.d("registerOnWorkerListener")
      listener?.let { listenerList.register(listener) }
    }

    override fun unregisterOnWorkerListener(listener: OnWorkerListener?) {
      Timber.d("unregisterOnWorkerListener")
      listenerList.unregister(listener)
    }
  }

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
    Global.applicationListJob = lifecycleScope.launch(Dispatchers.IO) {
      AppItemRepository.allApplicationInfoItems = getAppsList()
      Global.applicationListJob = null
    }.also {
      it.start()
    }
  }

  private suspend fun getAppsList(): List<ApplicationInfo> {
    var appList: List<ApplicationInfo>?

    do {
      appList = try {
        PackageUtils.getInstallApplications()
      } catch (e: Exception) {
        Timber.w(e)
        delay(GET_INSTALL_APPS_RETRY_PERIOD)
        null
      }?.also {
        AppItemRepository.allApplicationInfoItems = it
      }
    } while (appList == null)

    val pmList = mutableListOf<String>()
    try {
      val process = runtime.exec("pm list packages")
      InputStreamReader(process.inputStream, StandardCharsets.UTF_8).use { isr ->
        BufferedReader(isr).use { br ->
          br.forEachLine { line ->
            line.trim().let { trimLine ->
              if (trimLine.length > 8 && trimLine.startsWith("package:")) {
                trimLine.substring(8).let {
                  if (it.isNotEmpty()) {
                    pmList.add(it)
                  }
                }
              }
            }
          }
        }
      }
      if (pmList.size > appList.size) {
        appList = pmList.asSequence()
          .map { PackageUtils.getPackageInfo(it).applicationInfo }
          .toList()
      }
    } catch (t: Throwable) {
      Timber.w(t)
      appList = emptyList()
    }
    return appList!!
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
}
