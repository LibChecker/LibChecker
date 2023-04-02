package com.absinthe.libchecker.ui.base

import android.content.Context
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import com.absinthe.libchecker.utils.OsUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class BaseAlertDialogBuilder : MaterialAlertDialogBuilder {
  constructor(context: Context) : super(context)
  constructor(context: Context, overrideThemeResId: Int) : super(context, overrideThemeResId)

  override fun create(): AlertDialog {
    return super.create().also { dialog ->
      if (OsUtils.atLeastS()) {
        dialog.window?.let {
          it.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
          it.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
          it.attributes.blurBehindRadius = 64
        }
      }
    }
  }
}
