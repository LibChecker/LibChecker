package com.absinthe.libchecker.ui.app

import android.content.pm.PackageManager
import androidx.viewbinding.ViewBinding
import com.absinthe.libchecker.base.BaseActivity
import com.absinthe.libchecker.utils.PackageUtils

abstract class CheckPackageOnResumingActivity<VB : ViewBinding> : BaseActivity<VB>() {
  abstract fun requirePackageName(): String?

  override fun onResume() {
    super.onResume()
    requirePackageName()?.let {
      try {
        PackageUtils.getPackageInfo(it)
      } catch (e: PackageManager.NameNotFoundException) {
        finish()
      }
    } ?: finish()
  }
}
