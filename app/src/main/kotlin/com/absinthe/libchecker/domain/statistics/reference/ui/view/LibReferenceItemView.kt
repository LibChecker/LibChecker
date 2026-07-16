package com.absinthe.libchecker.domain.statistics.reference.ui.view

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.buildSpannedString
import androidx.core.text.italic
import androidx.core.view.children
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceItemDisplay
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.utils.extensions.tintHighlightText
import com.absinthe.libchecker.view.AViewGroup

class LibReferenceItemView(context: Context) : FrameLayout(context) {

  private val container = LibReferenceItemContainerView(context).apply {
    val padding = context.getDimensionPixelSize(R.dimen.main_card_padding)
    setPadding(padding, padding, padding, padding)
    setBackgroundResource(context.getResourceIdByAttr(android.R.attr.selectableItemBackground))
  }

  init {
    addView(container)
  }

  fun bind(
    display: LibReferenceItemDisplay,
    highlightText: String
  ) {
    container.bind(display, highlightText)
    contentDescription = display.contentDescription
  }

  class LibReferenceItemContainerView(context: Context) : AViewGroup(context) {

    private val icon = AppCompatImageButton(context).apply {
      id = android.R.id.icon
      val iconSize = context.getDimensionPixelSize(R.dimen.lib_reference_icon_size)
      layoutParams = FrameLayout.LayoutParams(iconSize, iconSize)
      setBackgroundResource(R.drawable.bg_circle_secondary_container)
      addView(this)
    }

    private val labelName = AppCompatTextView(
      ContextThemeWrapper(
        context,
        R.style.TextView_SansSerifMedium
      )
    ).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).also {
        it.marginStart = 8.dp
        it.marginEnd = 8.dp
      }
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
      addView(this)
    }

    private val libName =
      AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerif)).apply {
        layoutParams = LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        ).also {
          it.marginStart = 8.dp
          it.marginEnd = 8.dp
        }
        setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        addView(this)
      }

    private val count =
      AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerif)).apply {
        layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceHeadlineMedium))
        addView(this)
      }

    fun bind(
      display: LibReferenceItemDisplay,
      highlightText: String
    ) {
      icon.apply {
        setImageResource(display.iconRes)
        contentDescription = display.iconContentDescription
        importantForAccessibility = if (display.canOpenDetail) {
          View.IMPORTANT_FOR_ACCESSIBILITY_YES
        } else {
          View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        drawable?.mutate()?.colorFilter = if (display.desaturateIcon) {
          ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        } else {
          null
        }
      }
      if (display.italicLabel) {
        labelName.text = buildSpannedString {
          italic { append(display.label) }
          // Prevent italic text clipping.
          append(" ")
        }
      } else {
        setOrHighlightText(labelName, display.label, highlightText)
      }
      setOrHighlightText(libName, display.libName, highlightText)
      count.text = display.count
    }

    private fun setOrHighlightText(
      view: AppCompatTextView,
      text: CharSequence,
      highlightText: String
    ) {
      if (highlightText.isNotBlank()) {
        view.tintHighlightText(highlightText, text)
      } else {
        view.text = text
      }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      children.forEach {
        it.autoMeasure()
      }
      val labelWidth = (
        measuredWidth -
          paddingStart -
          paddingEnd -
          icon.measuredWidth -
          count.measuredWidth -
          labelName.marginLeft -
          labelName.marginRight
        )
      if (labelName.measuredWidth > labelWidth) {
        labelName.measure(labelWidth.toExactlyMeasureSpec(), labelName.defaultHeightMeasureSpec(this))
      }
      if (libName.measuredWidth > labelWidth) {
        libName.measure(labelWidth.toExactlyMeasureSpec(), libName.defaultHeightMeasureSpec(this))
      }
      setMeasuredDimension(
        measuredWidth,
        (paddingTop + labelName.measuredHeight + libName.measuredHeight + paddingBottom)
          .coerceAtLeast(icon.measuredHeight + paddingTop + paddingBottom)
      )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      icon.layout(paddingStart, icon.toVerticalCenter(this))
      labelName.layout(paddingStart + icon.measuredWidth + labelName.marginLeft, paddingTop)
      libName.layout(paddingStart + icon.measuredWidth + labelName.marginLeft, labelName.bottom)
      count.layout(paddingEnd, count.toVerticalCenter(this), fromRight = true)
    }
  }
}
