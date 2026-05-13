package com.absinthe.libchecker.features.applist.detail.ui

import android.app.ComponentCaller
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ALL
import com.absinthe.libchecker.annotation.isComponentType
import com.absinthe.libchecker.compat.IntentCompat
import com.absinthe.libchecker.features.applist.detail.IDetailContainer
import com.absinthe.libchecker.features.applist.detail.bean.DetailExtraBean
import com.absinthe.libchecker.features.statistics.ui.EXTRA_REF_NAME
import com.absinthe.libchecker.features.statistics.ui.EXTRA_REF_TYPE
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.extensions.isArchivedPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

const val EXTRA_PACKAGE_NAME = Intent.EXTRA_PACKAGE_NAME
const val EXTRA_DETAIL_BEAN = "EXTRA_DETAIL_BEAN"

class AppDetailActivity :
  BaseAppDetailActivity(),
  IDetailContainer {

  private var pkgName: String? = null
  private var refName: String? = null
  private var refType: Int? = null
  private var extraBean: DetailExtraBean? = null
  private var initPackageJob: Job? = null

  override val apkAnalyticsMode: Boolean = false
  override fun requirePackageName() = pkgName
  override fun getToolbar() = binding.toolbar

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    isPackageReady = true
    initPackage(intent)
  }

  override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
    super.onNewIntent(intent, caller)
    initPackage(intent)
  }

  override fun onPostPackageInfoAvailable() {
    resolveReferenceExtras()
  }

  override fun onStart() {
    super.onStart()
    registerPackageBroadcast()
  }

  override fun onStop() {
    super.onStop()
    unregisterPackageBroadcast()
  }

  private fun initPackage(intent: Intent) {
    pkgName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: let {
      intent.data?.let { uri ->
        uri.getQueryParameter("id").takeIf { uri.scheme == "market" && uri.host == "details" }
      }
    }
    refName = intent.getStringExtra(EXTRA_REF_NAME)
    refType = intent.getIntExtra(EXTRA_REF_TYPE, ALL)
    extraBean = IntentCompat.getParcelableExtra<DetailExtraBean>(intent, EXTRA_DETAIL_BEAN)

    Timber.d("packageName: $pkgName")
    val packageName = pkgName ?: return
    val detailExtraBean = extraBean
    initPackageJob?.cancel()
    initPackageJob = lifecycleScope.launch(Dispatchers.IO) {
      runCatching {
        val flag = PackageManager.GET_PERMISSIONS or PackageManager.GET_META_DATA
        PackageUtils.getPackageInfo(packageName, flag, false)
      }.onFailure {
        Timber.d("getPackageInfo: $packageName failed, %s", it.message)
        withContext(Dispatchers.Main) {
          finish()
        }
      }.onSuccess { packageInfo ->
        withContext(Dispatchers.Main) {
          if (packageInfo.isArchivedPackage()) {
            Timber.w("isArchivedPackage: $packageName")
            Toasty.showLong(this@AppDetailActivity, R.string.archived_app)
            finish()
          } else {
            onPackageInfoAvailable(packageInfo, detailExtraBean)
          }
        }
      }
    }
  }

  private fun resolveReferenceExtras() {
    if (pkgName == null || refName == null || refType == ALL) {
      return
    }
    navigateToReferenceComponentPosition(pkgName!!, refName!!)
  }

  private fun navigateToReferenceComponentPosition(packageName: String, refName: String) {
    val refType = refType ?: return
    val position = typeList.indexOf(refType)
    binding.viewpager.currentItem = position
    binding.tabLayout.post {
      val targetTab = binding.tabLayout.getTabAt(position)
      if (targetTab?.isSelected == false) {
        targetTab.select()
      }
    }

    val componentName = if (isComponentType(refType)) {
      refName.removePrefix(packageName)
    } else {
      refName
    }
    detailFragmentManager.navigateToComponent(refType, componentName)
  }

  private val requestPackageReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      val pkg = intent.data?.schemeSpecificPart.orEmpty()
      if (pkg == pkgName) {
        if (intent.action == Intent.ACTION_PACKAGE_REMOVED) {
          finish()
        } else {
          recreate()
        }
      }
    }
  }

  private fun registerPackageBroadcast() {
    val intentFilter = IntentFilter().apply {
      addAction(Intent.ACTION_PACKAGE_REPLACED)
      addAction(Intent.ACTION_PACKAGE_REMOVED)
      addDataScheme("package")
    }

    registerReceiver(requestPackageReceiver, intentFilter)
  }

  private fun unregisterPackageBroadcast() {
    unregisterReceiver(requestPackageReceiver)
  }
}
