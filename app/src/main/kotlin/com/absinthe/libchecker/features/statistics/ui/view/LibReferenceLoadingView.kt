package com.absinthe.libchecker.features.statistics.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.AUTUMN
import com.absinthe.libchecker.annotation.SPRING
import com.absinthe.libchecker.annotation.SUMMER
import com.absinthe.libchecker.annotation.WINTER
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.view.AViewGroup
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.google.android.material.progressindicator.LinearProgressIndicator

class LibReferenceLoadingView(
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
    val assetName = when (GlobalValues.season) {
      SPRING -> "anim/lib_reference_spring.json.zip"
      SUMMER -> "anim/lib_reference_summer.json.zip"
      AUTUMN -> "anim/lib_reference_autumn.json.zip"
      WINTER -> "anim/lib_reference_winter.json.zip"
      else -> throw IllegalArgumentException("Are you living on earth?")
    }

    enableMergePathsForKitKatAndAbove(true)
    setAnimation(assetName)
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
      measuredWidth,
      measuredHeight
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    loadingView.layout(loadingView.toHorizontalCenter(this), loadingView.toVerticalCenter(this))
    progressIndicator.layout(progressIndicator.toHorizontalCenter(this), loadingView.bottom)
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    loadingView.playAnimation()
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    loadingView.pauseAnimation()
  }
}
