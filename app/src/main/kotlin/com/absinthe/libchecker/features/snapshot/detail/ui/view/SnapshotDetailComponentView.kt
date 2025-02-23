package com.absinthe.libchecker.features.snapshot.detail.ui.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.toColorInt
import androidx.core.view.children
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.toColorStateList
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.rulesbundle.Rule
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip

class SnapshotDetailComponentView(context: Context) : MaterialCardView(context) {

  val container = SnapshotDetailComponentContainerView(context).apply {
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
  }

  init {
    addView(container)
  }

  class SnapshotDetailComponentContainerView(context: Context) : AViewGroup(context) {

    init {
      clipToPadding = false
      val padding = context.getDimensionPixelSize(R.dimen.main_card_padding)
      setPadding(padding, padding, padding, padding)
    }

    val typeIcon = AppCompatImageView(context).apply {
      layoutParams = LayoutParams(16.dp, 16.dp)
      imageTintList = R.color.material_blue_grey_700.toColorStateList(context)
      addView(this)
    }

    val name = AppCompatTextView(
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
      }
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
      setTextColor(Color.BLACK)
      addView(this)
    }

    private var chip: Chip? = null

    fun setChipOnClickListener(listener: OnClickListener?) {
      chip?.setOnClickListener(listener)
    }

    fun setChip(rule: Rule?, colorRes: Int) {
      if (rule == null) {
        if (chip != null) {
          removeView(chip)
          chip = null
        }
      } else {
        if (chip == null) {
          chip = Chip(context).apply {
            layoutParams = LayoutParams(
              ViewGroup.LayoutParams.WRAP_CONTENT,
              ViewGroup.LayoutParams.WRAP_CONTENT
            ).also {
              it.topMargin = 4.dp
            }
            setTextColor(Color.BLACK)
            chipStrokeColor = ColorStateList.valueOf(("#20000000".toColorInt()))
            chipStrokeWidth = 1.dp.toFloat()
            chipStartPadding = 10.dp.toFloat()
            setPadding(paddingStart, 2.dp, paddingEnd, 2.dp)
            addView(this)
          }
        }
        chip!!.apply {
          setChipIconResource(rule.iconRes)
          text = rule.label
          chipBackgroundColor = colorRes.toColorStateList(context)

          if (!GlobalValues.isColorfulIcon && !rule.isSimpleColorIcon) {
            val icon = chipIcon
            icon?.let {
              it.mutate().colorFilter =
                ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
              chipIcon = it
            }
          } else if (rule.isSimpleColorIcon) {
            chipIcon?.mutate()?.setTint(Color.BLACK)
          } else {
            setChipIconResource(rule.iconRes)
          }
        }
      }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      children.forEach {
        it.autoMeasure()
      }
      val textWidth = measuredWidth - paddingStart - typeIcon.measuredWidth - name.marginStart - paddingEnd
      if (name.measuredWidth > textWidth) {
        name.measure(textWidth.toExactlyMeasureSpec(), name.defaultHeightMeasureSpec(this))
      }
      val chipHeight = chip?.let { it.measuredHeight + it.marginTop } ?: 0
      setMeasuredDimension(
        measuredWidth,
        paddingTop + name.measuredHeight.coerceAtLeast(typeIcon.measuredHeight) + chipHeight + paddingBottom
      )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      typeIcon.layout(paddingStart, typeIcon.toVerticalCenter(this))
      val nameXOffset = paddingStart + typeIcon.measuredWidth + name.marginStart
      name.layout(nameXOffset, paddingTop)
      chip?.layout(nameXOffset, name.bottom + chip!!.marginTop)
    }
  }
}
