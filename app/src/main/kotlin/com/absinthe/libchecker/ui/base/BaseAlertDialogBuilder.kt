package com.absinthe.libchecker.ui.base

import android.content.Context
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.absinthe.libchecker.utils.OsUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class BaseAlertDialogBuilder : MaterialAlertDialogBuilder {
  constructor(context: Context) : super(context)
  constructor(context: Context, overrideThemeResId: Int) : super(context, overrideThemeResId)

  override fun create(): AlertDialog {
    return super.create().also { dialog ->
      if (OsUtils.atLeastS()) {
        dialog.window?.let { window ->
          val blurController = WindowBlurCompatController(dialog.context, window)

          window.decorView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
              blurController.start()
            }

            override fun onViewDetachedFromWindow(view: View) {
              blurController.finishWithAnimation(BLUR_ANIMATION_DURATION)
            }
          })

          dialog.setOnShowListener {
            blurController.animateBlurRadius(MAX_BLUR_RADIUS, BLUR_ANIMATION_DURATION)
          }
        }
      }
    }
  }

  private companion object {
    const val MAX_BLUR_RADIUS = 64f
    const val BLUR_ANIMATION_DURATION = 350L
  }
}
