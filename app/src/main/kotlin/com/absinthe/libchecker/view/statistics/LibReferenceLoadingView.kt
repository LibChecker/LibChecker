package com.absinthe.libchecker.view.statistics

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
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

class LibReferenceLoadingView(context: Context, attributeSet: AttributeSet? = null) :
  AViewGroup(context, attributeSet) {

  val loadingView = LottieAnimationView(context).apply {
    val size = context.getDimensionPixelSize(R.dimen.lottie_anim_size)
    layoutParams = FrameLayout.LayoutParams(size, size).also {
      it.gravity = Gravity.CENTER
    }
    imageAssetsFolder = "/"
    repeatCount = LottieDrawable.INFINITE
    val assetName = when (GlobalValues.season) {
      SPRING -> "lib_reference_spring.json"
      SUMMER -> "lib_reference_summer.json"
      AUTUMN -> "lib_reference_autumn.json"
      WINTER -> "lib_reference_winter.json"
      else -> throw IllegalArgumentException("Are you living on earth?")
    }

    setAnimation(assetName)
    addView(this)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    loadingView.autoMeasure()
    setMeasuredDimension(
      measuredWidth,
      measuredHeight
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    loadingView.layout(loadingView.toHorizontalCenter(this), loadingView.toVerticalCenter(this))
  }
}
