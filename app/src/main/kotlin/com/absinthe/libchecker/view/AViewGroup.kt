package com.absinthe.libchecker.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import com.absinthe.libchecker.utils.extensions.isRtl

/**
 * From drakeet
 */
abstract class AViewGroup(
  context: Context,
  attributeSet: AttributeSet? = null
) : ViewGroup(context, attributeSet) {

  protected fun View.defaultWidthMeasureSpec(parentView: ViewGroup): Int {
    return when (layoutParams.width) {
      ViewGroup.LayoutParams.MATCH_PARENT -> parentView.measuredWidth.toExactlyMeasureSpec()
      ViewGroup.LayoutParams.WRAP_CONTENT -> ViewGroup.LayoutParams.WRAP_CONTENT.toAtMostMeasureSpec()
      0 -> throw IllegalAccessException("Need special treatment for $this")
      else -> layoutParams.width.toExactlyMeasureSpec()
    }
  }

  protected fun View.defaultHeightMeasureSpec(parentView: ViewGroup): Int {
    return when (layoutParams.height) {
      ViewGroup.LayoutParams.MATCH_PARENT -> parentView.measuredHeight.toExactlyMeasureSpec()
      ViewGroup.LayoutParams.WRAP_CONTENT -> ViewGroup.LayoutParams.WRAP_CONTENT.toAtMostMeasureSpec()
      0 -> throw IllegalAccessException("Need special treatment for $this")
      else -> layoutParams.height.toExactlyMeasureSpec()
    }
  }

  protected fun Int.toExactlyMeasureSpec(): Int {
    return MeasureSpec.makeMeasureSpec(this, MeasureSpec.EXACTLY)
  }

  protected fun Int.toAtMostMeasureSpec(): Int {
    return MeasureSpec.makeMeasureSpec(this, MeasureSpec.AT_MOST)
  }

  protected fun View.autoMeasure() {
    measure(
      this.defaultWidthMeasureSpec(parentView = this@AViewGroup),
      this.defaultHeightMeasureSpec(parentView = this@AViewGroup)
    )
  }

  protected fun View.toHorizontalCenter(parentView: ViewGroup): Int {
    return (parentView.measuredWidth - measuredWidth) / 2
  }

  protected fun View.toVerticalCenter(parentView: ViewGroup): Int {
    return (parentView.measuredHeight - measuredHeight) / 2
  }

  protected fun View.toViewHorizontalCenter(targetView: View): Int {
    return targetView.left - (measuredWidth - targetView.measuredWidth) / 2
  }

  protected fun View.toViewVerticalCenter(targetView: View): Int {
    return targetView.top - (measuredHeight - targetView.measuredHeight) / 2
  }

  protected fun View.layout(x: Int, y: Int, fromRight: Boolean = false) {
    val actualFromRight = if (isRtl()) !fromRight else fromRight
    val actualX = if (actualFromRight) this@AViewGroup.measuredWidth - x - measuredWidth else x

    layout(actualX, y, actualX + measuredWidth, y + measuredHeight)
  }

  protected val Int.dp: Int get() = (this * resources.displayMetrics.density + 0.5f).toInt()
  protected val View.measuredWidthWithMargins get() = (measuredWidth + marginLeft + marginRight)
  protected val View.measuredHeightWithMargins get() = (measuredHeight + marginTop + marginBottom)

  protected class LayoutParams(width: Int, height: Int) : MarginLayoutParams(width, height)
}
