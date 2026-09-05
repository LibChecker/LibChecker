package com.absinthe.libchecker.view.app

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr

/** Centers the illustration and copy in the available area, scrolling only when they cannot fit. */
open class EmptyStateView(
  context: Context,
  @DrawableRes illustration: Int,
  @StringRes message: Int
) : ScrollView(context) {

  val text = AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerif)).apply {
    layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
      it.topMargin = 16.dp
    }
    gravity = Gravity.CENTER
    setText(message)
    setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceHeadlineSmall))
  }

  init {
    isFillViewport = true
    isVerticalScrollBarEnabled = false
    val content = LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      gravity = Gravity.CENTER
      setPadding(24.dp, 24.dp, 24.dp, 24.dp)
      layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
      addView(
        AppCompatImageView(context).apply {
          layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
          adjustViewBounds = true
          maxWidth = 160.dp
          maxHeight = 137.dp
          setImageResource(illustration)
          importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }
      )
      addView(text)
    }
    addView(content)
  }
}
