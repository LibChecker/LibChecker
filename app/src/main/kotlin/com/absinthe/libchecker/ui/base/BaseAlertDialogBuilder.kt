package com.absinthe.libchecker.ui.base

import android.animation.ValueAnimator
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

          if (OsUtils.atLeastT()) {
            val animator = ValueAnimator.ofInt(0, 13).apply {
              duration = 350
              addUpdateListener { animation ->
                if (!it.decorView.isAttachedToWindow) {
                  cancel()
                  return@addUpdateListener
                }
                val blurRadius = animation.animatedValue as Int * 5
                it.attributes.blurBehindRadius = blurRadius
                it.attributes = it.attributes
              }
            }

            dialog.setOnShowListener {
              animator.start()
            }
          } else {
            it.attributes.blurBehindRadius = 64
          }
        }
      }
    }
  }
}
