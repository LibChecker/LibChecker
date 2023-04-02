package com.absinthe.libchecker.ui.app

import androidx.viewbinding.ViewBinding
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.ui.base.BaseActivity
import com.absinthe.libchecker.utils.PackageUtils
import timber.log.Timber

abstract class CheckPackageOnResumingActivity<VB : ViewBinding> : BaseActivity<VB>() {
  abstract fun requirePackageName(): String?
  protected var isPackageReady: Boolean = false

  override fun onResume() {
    super.onResume()
    if (isPackageReady) {
      requirePackageName()?.let { pkgName ->
        runCatching {
          if (pkgName.endsWith(Constants.TEMP_PACKAGE)) {
            PackageManagerCompat.getPackageArchiveInfo(pkgName, 0)
          } else {
            PackageUtils.getPackageInfo(pkgName)
          }
        }.onFailure {
          Timber.d("requirePackageName: $pkgName failed" + it.message)
          finish()
        }
      }
    }
  }
}
