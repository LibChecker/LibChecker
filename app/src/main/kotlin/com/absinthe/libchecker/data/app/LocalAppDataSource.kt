package com.absinthe.libchecker.data.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.compat.PackageManagerCompat
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
  private val applications: MutableList<PackageInfo> = loadApplications().toMutableList()
  val apexPackageSet: Set<String> = loadApexPackageSet()

  private val _PackageChangeFlow: MutableSharedFlow<PackageChangeState> = MutableSharedFlow()
  val packageChangeFlow: SharedFlow<PackageChangeState> = _PackageChangeFlow.asSharedFlow()

  private var coroutineScope: CoroutineScope? = null
  private val pendingIntents = ArrayDeque<Intent>()

  private val packageReceiver by lazy {
    object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent?) {
        val intent = intent ?: return
        Timber.d("package receiver received: ${intent.action}, data: ${intent.data}")

        val packageName = intent.data?.encodedSchemeSpecificPart.orEmpty()
        intent.setPackage(packageName)

        pendingIntents.add(intent)

        coroutineScope?.launch {
          delay(1000)

          while (pendingIntents.isNotEmpty() && isActive) {
            val currentIntent = pendingIntents.removeFirst()
            val currentPackageName = currentIntent.`package`

            if (pendingIntents.none { it.`package` == currentPackageName }) {
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
    if (forceUpdate) {
      applicationsLock.write {
        applications.clear()
        applications.addAll(loadApplications())
      }
    }

    return applicationsLock.read {
      applications.toList()
    }
  }

  override fun getApplicationMap(forceUpdate: Boolean): Map<String, PackageInfo> {
    return getApplicationList(forceUpdate).asSequence()
      .filter { it.applicationInfo?.sourceDir != null || it.applicationInfo?.publicSourceDir != null || it.isArchivedPackage() }
      .map { it.packageName to it }
      .toMap()
  }

  private fun loadApplications(): List<PackageInfo> {
    Timber.d("loadApplications start")
    val flag = if (OsUtils.atLeastV()) PackageManager.MATCH_ARCHIVED_PACKAGES else 0
    val list = PackageManagerCompat.getInstalledPackages(flag)
    Timber.d("loadApplications end, apps count: ${list.size}")
    return list
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
      when (state) {
        is PackageChangeState.Added -> {
          applications.add(state.getActualPackageInfo())
        }

        is PackageChangeState.Removed -> {
          applications.removeAll { it.packageName == state.getActualPackageInfo().packageName }
        }

        is PackageChangeState.Replaced -> {
          applications.removeAll { it.packageName == state.getActualPackageInfo().packageName }
          applications.add(state.getActualPackageInfo())
        }
      }
    }
  }

  private fun generatePackageChangeState(intent: Intent): PackageChangeState? {
    val packageName = intent.data?.encodedSchemeSpecificPart.orEmpty()
    val packageInfo =
      runCatching { PackageUtils.getPackageInfo(packageName) }.getOrNull()
        ?: PackageInfo().apply { this.packageName = packageName }
    return when (intent.action) {
      Intent.ACTION_PACKAGE_ADDED -> PackageChangeState.Added(packageInfo)
      Intent.ACTION_PACKAGE_REMOVED -> PackageChangeState.Removed(packageInfo)
      Intent.ACTION_PACKAGE_REPLACED -> PackageChangeState.Replaced(packageInfo)
      else -> null
    }
  }
}
