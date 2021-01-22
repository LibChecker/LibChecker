package com.absinthe.libchecker.ui.app

import com.absinthe.libchecker.BaseActivity
import com.blankj.utilcode.util.AppUtils

abstract class CheckPackageOnResumingActivity : BaseActivity() {
    abstract fun requirePackageName(): String?

    override fun onResume() {
        super.onResume()
        if (!AppUtils.isAppInstalled(requirePackageName())) {
            finish()
        }
    }
}