package com.absinthe.libchecker.data.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.domain.app.PackageChangeState
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.isArchivedPackage
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

object LocalAppDataSource : AppDataSource {

  private val applicationsLock = ReentrantReadWriteLock()
  private val applicationMap: MutableMap<String, PackageInfo> = linkedMapOf()
  private var applicationListSnapshot: List<PackageInfo> = emptyList()
  private var applicationMapSnapshot: Map<String, PackageInfo> = emptyMap()
  private var applicationsLoaded: Boolean = false
  val apexPackageSet: Set<String> by lazy { loadApexPackageSet() }

  private val _PackageChangeFlow: MutableSharedFlow<PackageChangeState> = MutableSharedFlow()
  val packageChangeFlow: SharedFlow<PackageChangeState> = _PackageChangeFlow.asSharedFlow()

  private var coroutineScope: CoroutineScope? = null
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
                updateApplications(state)
                _PackageChangeFlow.emit(state)
              }
            }
          }
        }
      }
    }
  }

  fun addLifecycleOwner(owner: LifecycleOwner) {
    coroutineScope = owner.lifecycleScope

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

  fun removeLifecycleOwner(owner: LifecycleOwner) {
    coroutineScope = null
    runCatching {
      (owner as? Context)?.unregisterReceiver(packageReceiver)
    }
  }

  override fun getApplicationList(forceUpdate: Boolean): List<PackageInfo> {
    ensureApplicationsLoaded(forceUpdate)
    return applicationsLock.read {
      applicationListSnapshot
    }
  }

  override fun getApplicationMap(forceUpdate: Boolean): Map<String, PackageInfo> {
    ensureApplicationsLoaded(forceUpdate)
    return applicationsLock.read {
      applicationMapSnapshot
    }
  }

  fun getApplicationCount(forceUpdate: Boolean = false): Int {
    ensureApplicationsLoaded(forceUpdate)
    return applicationsLock.read {
      applicationMap.size
    }
  }

  fun getRandomApplicationInfo(forceUpdate: Boolean = false): ApplicationInfo? {
    ensureApplicationsLoaded(forceUpdate)
    return applicationsLock.read {
      applicationListSnapshot.randomOrNull()?.applicationInfo
    }
  }

  private fun ensureApplicationsLoaded(forceUpdate: Boolean = false) {
    if (!forceUpdate && applicationsLock.read { applicationsLoaded }) {
      return
    }
    applicationsLock.write {
      if (forceUpdate || !applicationsLoaded) {
        refreshApplicationsLocked()
      }
    }
  }

  private fun refreshApplicationsLocked() {
    applicationMap.clear()
    loadApplications()
      .asSequence()
      .filter { it.isVisiblePackageInfo() }
      .forEach { applicationMap[it.packageName] = it }
    updateSnapshotsLocked()
    applicationsLoaded = true
  }

  private fun loadApplications(): List<PackageInfo> {
    Timber.d("loadApplications start")
    val list = PackageManagerCompat.getInstalledPackages(getInstalledPackageFlags())
    Timber.d("loadApplications end, apps count: ${list.size}")
    return list
  }

  private fun getInstalledPackageFlags(): Long {
    return if (OsUtils.atLeastV()) PackageManager.MATCH_ARCHIVED_PACKAGES else 0L
  }

  private fun loadPackageInfo(packageName: String): PackageInfo? {
    return runCatching { PackageUtils.getPackageInfo(packageName) }.getOrNull()
  }

  private fun PackageInfo.isVisiblePackageInfo(): Boolean {
    return applicationInfo?.sourceDir != null || applicationInfo?.publicSourceDir != null || isArchivedPackage()
  }

  /**
   * Load apex package set
   * PackageInfo#isApex is always false
   * use this method to workaround
   */
  private fun loadApexPackageSet(): Set<String> {
    if (OsUtils.atLeastQ()) {
      Timber.d("loadApexPackageSet start")
      return runCatching {
        SystemServices.packageManager.getInstalledModules(PackageManager.MATCH_ALL)
          .map { it.packageName.orEmpty() }
          .toSet()
      }.onFailure {
        Timber.e(it)
      }.onSuccess {
        Timber.d("loadApexPackageSet end, apex count: ${it.size}")
      }.getOrDefault(emptySet())
    }
    return emptySet()
  }

  private fun updateApplications(state: PackageChangeState) {
    applicationsLock.write {
      if (!applicationsLoaded) {
        refreshApplicationsLocked()
      }
      val packageName = state.packageName
      when (state) {
        is PackageChangeState.Added -> {
          val packageInfo = loadPackageInfo(packageName)
          if (packageInfo?.isVisiblePackageInfo() == true) {
            applicationMap[packageName] = packageInfo
          } else {
            applicationMap.remove(packageName)
          }
        }

        is PackageChangeState.Removed -> {
          applicationMap.remove(packageName)
        }

        is PackageChangeState.Replaced -> {
          val packageInfo = loadPackageInfo(packageName)
          if (packageInfo?.isVisiblePackageInfo() == true) {
            applicationMap[packageName] = packageInfo
          } else {
            applicationMap.remove(packageName)
          }
        }
      }
      updateSnapshotsLocked()
    }
  }

  private fun updateSnapshotsLocked() {
    applicationListSnapshot = applicationMap.values.toList()
    applicationMapSnapshot = applicationMap.toMap()
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
