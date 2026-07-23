package com.absinthe.libchecker.services

import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.domain.app.list.usecase.InitializePendingAppFeaturesUseCase
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import java.lang.ref.WeakReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber

class WorkerService : LifecycleService() {

  private val initializePendingAppFeatures: InitializePendingAppFeaturesUseCase by inject()
  private val installedAppRepository: InstalledAppRepository by inject()
  private val binder by lazy { WorkerBinder(this) }
  private var initFeaturesJob: Job? = null
  private var pendingInitFeaturesRequest = false

  override fun onBind(intent: Intent): IBinder {
    super.onBind(intent)
    return binder
  }

  override fun onCreate() {
    super.onCreate()
    Timber.d("onCreate")
    updateFeatureInitializationState(running = false)
    installedAppRepository.startPackageChangeMonitoring(this)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.`package` != packageName) {
      stopSelf()
    }
    return super.onStartCommand(intent, flags, startId)
  }

  override fun onDestroy() {
    Timber.d("onDestroy")
    installedAppRepository.stopPackageChangeMonitoring(this)
    super.onDestroy()
  }

  @Synchronized
  private fun initFeatures() {
    if (initFeaturesJob?.isActive == true) {
      Timber.d("initFeatures queued")
      pendingInitFeaturesRequest = true
      return
    }

    startInitFeaturesLocked()
  }

  private fun startInitFeaturesLocked() {
    Timber.d("initFeatures")
    pendingInitFeaturesRequest = false
    updateFeatureInitializationState(running = true, completed = false)

    initFeaturesJob = lifecycleScope.launch(Dispatchers.IO) {
      try {
        initPendingFeatures()
      } finally {
        finishInitFeatures()
      }
    }
  }

  private suspend fun initPendingFeatures() {
    initializePendingAppFeatures()
  }

  @Synchronized
  private fun finishInitFeatures() {
    initFeaturesJob = null
    if (pendingInitFeaturesRequest) {
      startInitFeaturesLocked()
    } else {
      updateFeatureInitializationState(running = false, completed = true)
      Timber.d("initFeatures finished")
    }
  }

  class WorkerBinder(service: WorkerService) : IWorkerService.Stub() {

    private val serviceRef: WeakReference<WorkerService> = WeakReference(service)

    override fun initFeatures() {
      serviceRef.get()?.initFeatures()
    }
  }

  companion object {
    data class FeatureInitializationState(
      val running: Boolean = false,
      val completed: Boolean = false
    )

    private val _featureInitializationState = MutableStateFlow(FeatureInitializationState())
    val featureInitializationState = _featureInitializationState.asStateFlow()

    val initializingFeatures: Boolean
      get() = featureInitializationState.value.running

    private fun updateFeatureInitializationState(
      running: Boolean,
      completed: Boolean = featureInitializationState.value.completed
    ) {
      _featureInitializationState.value = FeatureInitializationState(running, completed)
    }
  }
}
