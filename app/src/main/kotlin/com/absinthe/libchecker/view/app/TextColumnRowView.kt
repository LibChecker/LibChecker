package com.absinthe.libchecker.view.app

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.expandChildTouchTarget

open class TextColumnRowView(context: Context) : LinearLayout(context) {

  private val textColumn = LinearLayout(context).apply {
    orientation = VERTICAL
    layoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
      gravity = Gravity.TOP
    }
  }
  private var expandedTouchTarget: View? = null
  private var expandedTouchTargetSize = 0

  init {
    orientation = HORIZONTAL
    gravity = Gravity.TOP
    addView(textColumn)
  }

  protected fun addTextLine(
    @StyleRes styleRes: Int = 0,
    configure: AppCompatTextView.() -> Unit = {}
  ): AppCompatTextView {
    return createTextView(styleRes).apply {
      layoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      configure()
      textColumn.addView(this)
    }
  }

  protected fun createTextView(
    @StyleRes styleRes: Int = 0,
    configure: AppCompatTextView.() -> Unit = {}
  ): AppCompatTextView {
    return AppCompatTextView(
      if (styleRes == 0) context else ContextThemeWrapper(context, styleRes)
    ).apply {
      configure()
    }
  }

  protected fun setLeadingView(view: View, spacing: Int = 8.dp) {
    addView(
      view,
      0,
      view.rowLayoutParams(Gravity.CENTER_VERTICAL).apply {
        marginEnd = spacing
      }
    )
  }

  protected fun setTrailingView(
    view: View,
    spacing: Int = 8.dp,
    gravity: Int = Gravity.CENTER_VERTICAL,
    touchTargetSize: Int = 0
  ) {
    addView(
      view,
      view.rowLayoutParams(gravity).apply {
        marginStart = spacing
      }
    )
    if (touchTargetSize > 0) {
      expandedTouchTarget = view
      expandedTouchTargetSize = touchTargetSize
    }
  }

  protected fun centerTextColumn() {
    (textColumn.layoutParams as LayoutParams).gravity = Gravity.CENTER_VERTICAL
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val fullWidthSpec = if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST) {
      MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY)
    } else {
      widthMeasureSpec
    }
    super.onMeasure(fullWidthSpec, heightMeasureSpec)
  }

  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    super.onLayout(changed, left, top, right, bottom)
    val target = expandedTouchTarget
    if (target?.isVisible == true) {
      expandChildTouchTarget(target, expandedTouchTargetSize)
    } else if (target != null) {
      touchDelegate = null
    }
  }

  private fun View.rowLayoutParams(gravity: Int): LayoutParams {
    val source = layoutParams
    return LayoutParams(
      source?.width ?: ViewGroup.LayoutParams.WRAP_CONTENT,
      source?.height ?: ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply {
      this.gravity = gravity
      if (source is ViewGroup.MarginLayoutParams) {
        leftMargin = source.leftMargin
        topMargin = source.topMargin
        rightMargin = source.rightMargin
        bottomMargin = source.bottomMargin
        marginStart = source.marginStart
        marginEnd = source.marginEnd
      }
    }
  }
}
