package com.absinthe.libchecker.integrations.anywhere

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.utils.PackageUtils
import timber.log.Timber

private const val ANYWHERE_APPLICATION_ID = Constants.PackageNames.ANYWHERE_
private const val FIRST_SUPPORT_VERSION_CODE = 2020000
private const val ACTION_EDITOR = "com.absinthe.anywhere_.intent.action.EDITOR"
private const val EXTRA_PACKAGE_NAME = "EXTRA_PACKAGE_NAME"
private const val EXTRA_CLASS_NAME = "EXTRA_CLASS_NAME"

class AnywhereManager {

  fun launchActivityEditor(context: Context, packageName: String, className: String) {
    val fullClassName = if (className.startsWith(".")) {
      packageName + className
    } else {
      className
    }
    try {
      context.startActivity(
        Intent(ACTION_EDITOR)
          .putExtra(EXTRA_PACKAGE_NAME, packageName)
          .putExtra(EXTRA_CLASS_NAME, fullClassName)
      )
    } catch (e: ActivityNotFoundException) {
      Timber.e(e)
    }
  }

  companion object {
    val isSupportInteraction =
      PackageUtils.isAppInstalled(ANYWHERE_APPLICATION_ID) &&
        PackageUtils.getVersionCode(ANYWHERE_APPLICATION_ID) >= FIRST_SUPPORT_VERSION_CODE
  }
}
