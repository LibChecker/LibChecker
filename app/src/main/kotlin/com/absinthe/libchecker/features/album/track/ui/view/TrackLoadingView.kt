package com.absinthe.libchecker.features.album.track.ui.view

import android.content.Context
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.view.AViewGroup
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import java.io.File

class TrackLoadingView(context: Context) : AViewGroup(context) {

  private val anim = LottieAnimationView(context).apply {
    val size = context.getDimensionPixelSize(R.dimen.lottie_anim_size)
    layoutParams = LayoutParams(size, size)
    imageAssetsFolder = File.separator
    repeatCount = LottieDrawable.INFINITE
    setAnimation("anim/track_target.json.zip")
    enableMergePathsForKitKatAndAbove(true)
    addView(this)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    anim.autoMeasure()
    setMeasuredDimension(measuredWidth, measuredHeight)
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    anim.layout(anim.toHorizontalCenter(this), anim.toVerticalCenter(this))
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    anim.playAnimation()
  }
}
