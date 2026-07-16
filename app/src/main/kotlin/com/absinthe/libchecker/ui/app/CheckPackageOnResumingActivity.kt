package com.absinthe.libchecker.ui.app

import androidx.viewbinding.ViewBinding
import com.absinthe.libchecker.domain.app.maintenance.CheckRequiredPackageAvailabilityUseCase
import com.absinthe.libchecker.ui.base.BaseActivity
import org.koin.android.ext.android.inject
import timber.log.Timber

abstract class CheckPackageOnResumingActivity<VB : ViewBinding> : BaseActivity<VB>() {
  private val checkRequiredPackageAvailability: CheckRequiredPackageAvailabilityUseCase by inject()

  abstract fun requirePackageName(): String?
  protected var isPackageReady: Boolean = false

  override fun onResume() {
    super.onResume()
    if (isPackageReady) {
      requirePackageName()?.let { pkgName ->
        if (!checkRequiredPackageAvailability(pkgName)) {
          Timber.d("requirePackageName: $pkgName failed")
          finish()
        }
      }
    }
  }
}
