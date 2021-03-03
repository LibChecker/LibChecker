package com.absinthe.libchecker.view

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

class FadePageTransformer : ViewPager2.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        if (position <= -1.0F || position >= 1.0F) {
            page.translationX = page.width * position
            page.alpha = 0.0F
        } else if (position == 0.0F) {
            page.translationX = page.width * position
            page.alpha = 1.0F
        } else {
            // position is between -1.0F & 0.0F OR 0.0F & 1.0F
            page.translationX = page.width * -position
            page.alpha = 1.0F - abs(position)
        }
    }
}