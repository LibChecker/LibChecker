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

class NativeLibItemView(context: Context) : AViewGroup(context) {

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
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
      addView(this)
    }

  val libSize =
    AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerifCondensed)).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
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

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    val chipWidth = chip?.apply { autoMeasure() }?.measuredWidth ?: 0
    if (chipWidth != 0) {
      chipWidth.plus(libName.marginEnd)
    }
    val libNameWidth = measuredWidth - paddingStart - paddingEnd - libName.marginEnd - chipWidth
    libName.measure(libNameWidth.toExactlyMeasureSpec(), libName.defaultHeightMeasureSpec(this))
    libSize.measure(libNameWidth.toExactlyMeasureSpec(), libSize.defaultHeightMeasureSpec(this))
    setMeasuredDimension(
      measuredWidth,
      (libName.measuredHeight + libSize.measuredHeight + paddingTop + paddingBottom).coerceAtLeast(
        40.dp
      )
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    libName.layout(paddingStart, paddingTop)
    libSize.layout(libName.left, libName.bottom)
    chip?.let { it.layout(paddingEnd, it.toVerticalCenter(this), fromRight = true) }
  }
}
