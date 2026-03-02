package com.absinthe.libchecker.features.applist.detail.ui.view

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.text.TextUtils
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.children
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.utils.extensions.displayWidth
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.visibleHeight
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.rulesbundle.Rule
import com.google.android.material.chip.Chip

class StaticLibItemView(context: Context) : AViewGroup(context) {

  init {
    isClickable = true
    isFocusable = true
    clipToPadding = false
    val horizontalPadding = context.getDimensionPixelSize(R.dimen.normal_padding)
    val verticalPadding = 4.dp
    setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
  }

  val libName =
    AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerifMedium)).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
      addView(this)
    }

  val libDetail =
    AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerifCondensed)).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
      addView(this)
    }

  private var chip: Chip? = null

  fun setChip(rule: Rule?) {
    chip = rule?.let {
      getOrCreateChip().apply {
        text = it.label
        setChipIconResource(it.iconRes)

        if (!GlobalValues.isColorfulIcon && !it.isSimpleColorIcon) {
          chipIcon?.let { icon ->
            icon.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
            chipIcon = icon
          }
        }
      }
    } ?: run {
      chip?.let { removeView(it) }
      null
    }
  }

  private fun getOrCreateChip() = chip ?: Chip(context).apply {
    isClickable = false
    layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 48.dp)
    maxWidth = (context.displayWidth * 0.45f).toInt()
    ellipsize = TextUtils.TruncateAt.MIDDLE
    addView(this)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    children.forEach {
      it.autoMeasure()
    }
    val libNameWidth = measuredWidth - paddingStart - paddingEnd
    if (libName.measuredWidth > libNameWidth) {
      libName.measure(libNameWidth.toExactlyMeasureSpec(), libName.defaultHeightMeasureSpec(this))
    }
    if (libDetail.measuredWidth > libNameWidth) {
      libDetail.measure(libNameWidth.toExactlyMeasureSpec(), libDetail.defaultHeightMeasureSpec(this))
    }
    setMeasuredDimension(
      measuredWidth,
      (libName.measuredHeight + libDetail.measuredHeight + chip.visibleHeight() + paddingTop + paddingBottom).coerceAtLeast(40.dp)
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    libName.layout(paddingStart, paddingTop)
    libDetail.layout(paddingStart, libName.bottom)
    chip?.layout(paddingStart, libDetail.bottom)
  }
}
