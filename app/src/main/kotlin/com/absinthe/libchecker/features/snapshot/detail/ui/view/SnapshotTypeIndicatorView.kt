package com.absinthe.libchecker.features.snapshot.detail.ui.view

import android.content.Context
import android.graphics.Outline
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginStart
import com.absinthe.libchecker.R
import com.absinthe.libchecker.view.AViewGroup

class SnapshotTypeIndicatorView(context: Context) : AViewGroup(context) {
  var enableRoundCorner: Boolean = true

  private val text =
    AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerifCondensed)).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
      addView(this)
    }

  private val icon = AppCompatImageView(context).apply {
    layoutParams = LayoutParams(16.dp, 16.dp).also {
      it.marginStart = 4.dp
    }
    addView(this)
  }

  private val colorLabel = View(context).apply {
    layoutParams = LayoutParams(20.dp, 5.dp).also {
      it.marginStart = 4.dp
    }
    addView(this)
  }

  fun setIndicatorInfo(str: String, iconDrawable: Drawable?, labelColorRes: Int) {
    text.text = str
    icon.setImageDrawable(iconDrawable)
    colorLabel.setBackgroundColor(labelColorRes)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    text.autoMeasure()
    icon.autoMeasure()
    colorLabel.autoMeasure()
    setMeasuredDimension(
      (
        text.measuredWidth +
          icon.marginStart + icon.measuredWidth +
          colorLabel.marginStart + colorLabel.measuredWidth
        ),
      text.measuredHeight.coerceAtLeast(icon.measuredHeight)
    )
    if (enableRoundCorner) {
      colorLabel.apply {
        outlineProvider = object : ViewOutlineProvider() {
          override fun getOutline(view: View?, outline: Outline?) {
            outline?.setRoundRect(
              0,
              0,
              measuredWidth,
              measuredHeight,
              (measuredWidth / 2).toFloat()
            )
          }
        }
        clipToOutline = true
      }
    }
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    text.layout(0, text.toVerticalCenter(this))
    icon.layout(text.measuredWidth + icon.marginStart, icon.toVerticalCenter(this))
    colorLabel.layout(text.measuredWidth + icon.measuredWidth + colorLabel.marginStart, colorLabel.toVerticalCenter(this))
  }
}
