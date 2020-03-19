package com.absinthe.libchecker.listener

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

class FadeOutPageTransformer : ViewPager2.PageTransformer {

    override fun transformPage(view: View, position: Float) {
        view.apply {
            translationX = width * -position

            alpha = if (position <= -1.0F || position >= 1.0F) {
                0.0F
            } else if (position == 0.0F) {
                1.0F
            } else {
                // position is between -1.0F & 0.0F OR 0.0F & 1.0F
                1.0F - abs(position)
            }
        }
    }
}