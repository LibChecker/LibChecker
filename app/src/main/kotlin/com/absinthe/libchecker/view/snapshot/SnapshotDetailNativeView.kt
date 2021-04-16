package com.absinthe.libchecker.view.snapshot

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
import androidx.core.content.ContextCompat
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.librarymap.IconResMap
import com.absinthe.libchecker.database.entity.RuleEntity
import com.absinthe.libchecker.extensions.getDimensionPixelSize
import com.absinthe.libchecker.extensions.valueUnsafe
import com.absinthe.libchecker.view.AViewGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip

class SnapshotDetailNativeView(context: Context) : MaterialCardView(context) {

    val container = SnapshotDetailNativeContainerView(context)

    init {
        addView(container)
    }

    class SnapshotDetailNativeContainerView(context: Context) : AViewGroup(context) {

        init {
            clipToPadding = false
            val padding = context.getDimensionPixelSize(R.dimen.main_card_padding)
            setPadding(padding, padding, padding, padding)
        }

        val typeIcon = AppCompatImageView(context).apply {
            layoutParams = LayoutParams(16.dp, 16.dp)
            imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.material_blue_grey_700))
            addView(this)
        }

        val name = AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerifMedium)).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                it.marginStart = 8.dp
            }
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(Color.BLACK)
            addView(this)
        }

        val libSize = AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerifCondensed)).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(Color.BLACK)
            addView(this)
        }

        private var chip: Chip? = null

        fun setChipOnClickListener(listener: OnClickListener?) {
            chip?.setOnClickListener(listener)
        }

        fun setChip(entity: RuleEntity?, colorRes: Int) {
            if (entity == null) {
                if (chip != null) {
                    removeView(chip)
                    chip = null
                }
            } else {
                if (chip == null) {
                    chip = Chip(ContextThemeWrapper(context, R.style.App_LibChip)).apply {
                        layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
                            it.topMargin = 4.dp
                        }
                        setTextColor(Color.BLACK)
                        chipStrokeColor = ColorStateList.valueOf((Color.parseColor("#20000000")))
                        chipStrokeWidth = 1.dp.toFloat()
                        chipStartPadding = 10.dp.toFloat()
                        setPadding(paddingStart, 2.dp, paddingEnd, 2.dp)
                        addView(this)
                    }
                }
                chip!!.apply {
                    setChipIconResource(IconResMap.getIconRes(entity.iconIndex))
                    text = entity.label
                    chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))

                    if (!GlobalValues.isColorfulIcon.valueUnsafe && !IconResMap.isSingleColorIcon(entity.iconIndex)) {
                        val icon = chipIcon
                        icon?.let {
                            it.mutate().colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                            chipIcon = it
                        }
                    } else if (IconResMap.isSingleColorIcon(entity.iconIndex)) {
                        chipIcon?.mutate()?.setTint(Color.BLACK)
                    } else {
                        setChipIconResource(IconResMap.getIconRes(entity.iconIndex))
                    }
                }
            }
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            typeIcon.autoMeasure()
            val textWidth = (measuredWidth - paddingStart - typeIcon.measuredWidth - name.marginStart - paddingEnd)
            name.measure(textWidth.toExactlyMeasureSpec(), name.defaultHeightMeasureSpec(this))
            libSize.measure(textWidth.toExactlyMeasureSpec(), libSize.defaultHeightMeasureSpec(this))
            chip?.autoMeasure()
            val chipHeight = if (chip != null) {
                chip!!.measuredHeight + chip!!.marginTop
            } else {
                0
            }
            setMeasuredDimension(measuredWidth, paddingTop + name.measuredHeight + libSize.measuredHeight + chipHeight + paddingBottom)
        }

        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
            typeIcon.layout(paddingStart, typeIcon.toVerticalCenter(this))
            name.layout(typeIcon.right + name.marginStart, (measuredHeight - name.measuredHeight - libSize.measuredHeight - (chip?.measuredHeight ?: 0)) / 2)
            libSize.layout(name.left, name.bottom)
            chip?.layout(name.left, libSize.bottom + chip!!.marginTop)
        }
    }
}