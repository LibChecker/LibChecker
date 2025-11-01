package com.absinthe.libchecker.features.applist.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.marginTop
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.app.AppsListLoadingView
import com.google.android.material.progressindicator.LinearProgressIndicator

class AppListInitialiseView(
  context: Context,
  attributeSet: AttributeSet? = null
) : AViewGroup(context, attributeSet) {

  val loadingView = AppsListLoadingView(context).apply {
    val size = context.getDimensionPixelSize(R.dimen.sphere_loading_size)
    layoutParams = FrameLayout.LayoutParams(size, size).also {
      it.gravity = Gravity.CENTER
    }
    setPreloadedBitmap(AppsListLoadingView.TextureType.APPS)
    addView(this)
  }

  val progressIndicator = LinearProgressIndicator(context).apply {
    layoutParams = LayoutParams(200.dp, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
      topMargin = 16.dp
    }
    trackCornerRadius = 3.dp
    addView(this)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    loadingView.autoMeasure()
    progressIndicator.autoMeasure()
    setMeasuredDimension(
      loadingView.measuredWidth.coerceAtLeast(progressIndicator.measuredWidth),
      loadingView.measuredHeight + progressIndicator.measuredHeight + progressIndicator.marginTop
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    loadingView.layout(loadingView.toHorizontalCenter(this), 0)
    progressIndicator.layout(progressIndicator.toHorizontalCenter(this), loadingView.bottom + progressIndicator.marginTop)
  }
}
