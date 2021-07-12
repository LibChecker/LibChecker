package com.absinthe.libchecker.view.snapshot

import android.content.Context
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.view.AViewGroup
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable

class TrackLoadingView(context: Context) : AViewGroup(context) {

    private val anim = LottieAnimationView(context).apply {
        val size = context.getDimensionPixelSize(R.dimen.lottie_anim_size)
        layoutParams = LayoutParams(size, size)
        imageAssetsFolder = "/"
        repeatCount = LottieDrawable.INFINITE
        setAnimation("track_target.json")
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
