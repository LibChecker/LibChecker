package com.absinthe.libchecker.ui.app

import androidx.viewbinding.ViewBinding
import com.absinthe.libchecker.base.BaseActivity
import com.absinthe.libchecker.utils.PackageUtils

abstract class CheckPackageOnResumingActivity<VB : ViewBinding> : BaseActivity<VB>() {
  abstract fun requirePackageName(): String?

  override fun onResume() {
    super.onResume()
    try {
      requirePackageName()?.let {
        PackageUtils.getPackageInfo(it)
      } ?: finish()
    } catch (e: Exception) {
      finish()
    }
  }
}
