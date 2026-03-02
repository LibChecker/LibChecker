package com.absinthe.libchecker.features.applist.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.view.app.RingDotsView
import com.google.android.material.progressindicator.CircularProgressIndicator

class AppListInitialiseView(
  context: Context,
  attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet) {

  init {
    clipChildren = false
  }

  val loadingView = RingDotsView(context).apply {
    val size = context.getDimensionPixelSize(R.dimen.general_loading_size)
    layoutParams = LayoutParams(size, size).also {
      it.gravity = Gravity.CENTER
    }
    addView(this)
  }

  val progressIndicator = CircularProgressIndicator(context).apply {
    layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).also {
      it.gravity = Gravity.CENTER
    }
    addView(this)
  }
}
