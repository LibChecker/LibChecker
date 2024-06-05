package com.absinthe.libchecker.features.applist.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.view.AViewGroup
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.google.android.material.progressindicator.LinearProgressIndicator

class AppListInitialiseView(
  context: Context,
  attributeSet: AttributeSet? = null
) : AViewGroup(context, attributeSet) {

  val loadingView = LottieAnimationView(context).apply {
    val size = context.getDimensionPixelSize(R.dimen.lottie_anim_size)
    layoutParams = FrameLayout.LayoutParams(size, size).also {
      it.gravity = Gravity.CENTER
    }
    imageAssetsFolder = "/"
    repeatCount = LottieDrawable.INFINITE
    setAnimation("anim/app_list_loading.json")
    addView(this)
  }

  val progressIndicator = LinearProgressIndicator(context).apply {
    layoutParams = LayoutParams(200.dp, ViewGroup.LayoutParams.WRAP_CONTENT)
    trackCornerRadius = 3.dp
    addView(this)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    loadingView.autoMeasure()
    progressIndicator.autoMeasure()
    setMeasuredDimension(
      loadingView.measuredWidth.coerceAtLeast(progressIndicator.measuredWidth),
      loadingView.measuredHeight + progressIndicator.measuredHeight
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    loadingView.layout(loadingView.toHorizontalCenter(this), 0)
    progressIndicator.layout(progressIndicator.toHorizontalCenter(this), loadingView.bottom)
  }
}
