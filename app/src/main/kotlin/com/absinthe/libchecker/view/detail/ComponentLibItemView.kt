package com.absinthe.libchecker.view.detail

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginEnd
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.LibChip
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.valueUnsafe
import com.absinthe.libchecker.view.AViewGroup
import com.google.android.material.chip.Chip

class ComponentLibItemView(context: Context) : AViewGroup(context) {

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
      ).also {
        it.marginEnd = context.getDimensionPixelSize(R.dimen.normal_padding)
      }
      setTextColor(context.getColorByAttr(R.attr.colorOnSurface))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
      addView(this)
    }

  private var chip: Chip? = null

  fun setChip(libChip: LibChip?) {
    if (libChip == null) {
      chip?.let {
        removeView(it)
        chip = null
      }
    } else {
      if (chip == null) {
        chip = Chip(ContextThemeWrapper(context, R.style.App_LibChip)).apply {
          isClickable = false
          layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 48.dp)
          addView(this)
        }
      }
      chip!!.apply {
        text = libChip.name
        setChipIconResource(libChip.iconRes)

        if (!GlobalValues.isColorfulIcon.valueUnsafe) {
          val icon = chipIcon
          icon?.let {
            it.colorFilter =
              ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
            chipIcon = it
          }
        }
      }
    }
  }

  private var shouldBreakLines = false

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    var chipWidth = chip?.apply { autoMeasure() }?.measuredWidth ?: 0

    if (chipWidth > (measuredWidth * 4 / 7)) {
      chipWidth = 0
      shouldBreakLines = true
    } else {
      shouldBreakLines = false
    }

    val libNameWidth = measuredWidth - paddingStart - paddingEnd - libName.marginEnd - chipWidth
    libName.measure(libNameWidth.toExactlyMeasureSpec(), libName.defaultHeightMeasureSpec(this))
    val height = if (shouldBreakLines) {
      libName.measuredHeight + paddingTop + paddingBottom + (chip?.measuredHeight ?: 0)
    } else {
      libName.measuredHeight + paddingTop + paddingBottom
    }.coerceAtLeast(40.dp)
    setMeasuredDimension(measuredWidth, height)
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    if (shouldBreakLines) {
      libName.layout(paddingStart, paddingTop)
      chip?.layout(libName.left, libName.bottom)
    } else {
      libName.layout(paddingStart, libName.toVerticalCenter(this))
      chip?.let {
        it.layout(paddingEnd, it.toVerticalCenter(this), fromRight = true)
      }
    }
  }
}
