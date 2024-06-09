package com.absinthe.libchecker.features.applist.detail.ui

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import com.absinthe.libchecker.annotation.ALL
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.compat.IntentCompat
import com.absinthe.libchecker.features.applist.detail.IDetailContainer
import com.absinthe.libchecker.features.applist.detail.bean.DetailExtraBean
import com.absinthe.libchecker.features.statistics.ui.EXTRA_REF_NAME
import com.absinthe.libchecker.features.statistics.ui.EXTRA_REF_TYPE
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import timber.log.Timber

@SuppressLint("InlinedApi")
const val EXTRA_PACKAGE_NAME = Intent.EXTRA_PACKAGE_NAME
const val EXTRA_DETAIL_BEAN = "EXTRA_DETAIL_BEAN"

class AppDetailActivity :
  BaseAppDetailActivity(),
  IDetailContainer {

  private val pkgName by unsafeLazy {
    intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: let {
      intent.data?.let { uri ->
        uri.getQueryParameter("id").takeIf { uri.scheme == "market" && uri.host == "details" }
      }
    }
  }
  private val refName by unsafeLazy {
    intent.getStringExtra(EXTRA_REF_NAME)
  }
  private val refType by unsafeLazy {
    intent.getIntExtra(EXTRA_REF_TYPE, ALL)
  }
  private val extraBean by unsafeLazy {
    IntentCompat.getParcelableExtra<DetailExtraBean>(
      intent,
      EXTRA_DETAIL_BEAN
    )
  }

  override val apkAnalyticsMode: Boolean = false
  override fun requirePackageName() = pkgName
  override fun getToolbar() = binding.toolbar

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    isPackageReady = true
    Timber.d("packageName: $pkgName")
    val packageName = pkgName ?: return
    runCatching {
      @Suppress("InlinedApi")
      val flag = (
        PackageManager.GET_PERMISSIONS
          or PackageManager.GET_META_DATA
          or PackageManager.MATCH_DISABLED_COMPONENTS
          or PackageManager.MATCH_UNINSTALLED_PACKAGES
        )
      PackageUtils.getPackageInfo(packageName, flag)
    }.onFailure {
      Timber.d("getPackageInfo: $packageName failed, " + it.message)
      finish()
    }.onSuccess { packageInfo ->
      onPackageInfoAvailable(packageInfo, extraBean)
    }
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

  private fun resolveReferenceExtras() {
    if (pkgName == null || refName == null || refType == ALL) {
      return
    }
    navigateToReferenceComponentPosition(pkgName!!, refName!!)
  }

  private fun navigateToReferenceComponentPosition(packageName: String, refName: String) {
    val position = typeList.indexOf(refType)
    binding.viewpager.currentItem = position
    binding.tabLayout.post {
      val targetTab = binding.tabLayout.getTabAt(position)
      if (targetTab?.isSelected == false) {
        targetTab.select()
      }
    }

    val componentName = if (refType == PERMISSION) {
      refName
    } else {
      refName.removePrefix(packageName)
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
