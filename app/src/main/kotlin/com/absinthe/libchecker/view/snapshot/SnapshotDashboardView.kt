package com.absinthe.libchecker.view.snapshot

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginTop
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup
import com.google.android.material.card.MaterialCardView

class SnapshotDashboardView(context: Context) : MaterialCardView(context) {

    val container = SnapshotDashboardContainerView(context).apply {
        val padding = context.getDimensionPixelSize(R.dimen.normal_padding)
        setPadding(padding, padding, padding, padding)
    }

    init {
        addView(container)
    }

    class SnapshotDashboardContainerView(context: Context) : AViewGroup(context) {

        private val tvSnapshotTimestampTitle =
            AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerif)).apply {
                layoutParams = LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                text = context.getString(R.string.snapshot_current_timestamp)
            }

        val tvSnapshotTimestampText =
            AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerifBlack)).apply {
                layoutParams = LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setTextColor(context.getColor(R.color.textNormal))
                setBackgroundResource(context.getResourceIdByAttr(android.R.attr.selectableItemBackgroundBorderless))
            }

        val arrow = AppCompatImageView(context).apply {
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setImageResource(R.drawable.ic_arrow_drop_down)
            setBackgroundResource(context.getResourceIdByAttr(android.R.attr.selectableItemBackgroundBorderless))
        }

        private val tvSnapshotAppsCountTitle =
            AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerif)).apply {
                layoutParams = LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also {
                    it.topMargin = 5.dp
                }
                text = context.getString(R.string.snapshot_apps_count)
            }

        val tvSnapshotAppsCountText =
            AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerifBlack)).apply {
                layoutParams = LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setTextColor(context.getColor(R.color.textNormal))
            }

        private val addedIndicator = SnapshotTypeIndicatorView(context).apply {
            setIndicatorInfo(
                context.getString(R.string.snapshot_indicator_added),
                context.getDrawable(R.drawable.ic_add),
                context.getColor(R.color.material_green_300)
            )
        }

        private val removedIndicator = SnapshotTypeIndicatorView(context).apply {
            setIndicatorInfo(
                context.getString(R.string.snapshot_indicator_removed),
                context.getDrawable(R.drawable.ic_remove),
                context.getColor(R.color.material_red_300)
            )
        }

        private val changedIndicator = SnapshotTypeIndicatorView(context).apply {
            setIndicatorInfo(
                context.getString(R.string.snapshot_indicator_changed),
                context.getDrawable(R.drawable.ic_changed),
                context.getColor(R.color.material_yellow_300)
            )
        }

        private val movedIndicator = SnapshotTypeIndicatorView(context).apply {
            setIndicatorInfo(
                context.getString(R.string.snapshot_indicator_moved),
                context.getDrawable(R.drawable.ic_move),
                context.getColor(R.color.material_blue_300)
            )
        }

        init {
            background = null
            addView(tvSnapshotTimestampTitle)
            addView(tvSnapshotTimestampText)
            addView(arrow)
            addView(tvSnapshotAppsCountTitle)
            addView(tvSnapshotAppsCountText)
            addView(addedIndicator)
            addView(removedIndicator)
            addView(changedIndicator)
            addView(movedIndicator)
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            addedIndicator.autoMeasure()
            removedIndicator.autoMeasure()
            changedIndicator.autoMeasure()
            movedIndicator.autoMeasure()
            arrow.autoMeasure()
            val indicatorsHeight = addedIndicator.measuredHeight * 4
            val indicatorsWidth = listOf(
                addedIndicator.measuredWidth,
                removedIndicator.measuredWidth,
                changedIndicator.measuredWidth,
                movedIndicator.measuredWidth
            ).maxOrNull()!!

            val textWidth = measuredWidth - paddingStart - paddingEnd - indicatorsWidth
            tvSnapshotTimestampTitle.let {
                it.measure(
                    textWidth.toExactlyMeasureSpec(),
                    it.defaultHeightMeasureSpec(this)
                )
            }
            tvSnapshotTimestampText.let {
                it.measure(
                    it.defaultWidthMeasureSpec(this),
                    it.defaultHeightMeasureSpec(this)
                )
            }
            tvSnapshotAppsCountTitle.let {
                it.measure(
                    textWidth.toExactlyMeasureSpec(),
                    it.defaultHeightMeasureSpec(this)
                )
            }
            tvSnapshotAppsCountText.let {
                it.measure(
                    it.defaultWidthMeasureSpec(this),
                    it.defaultHeightMeasureSpec(this)
                )
            }

            setMeasuredDimension(
                measuredWidth,
                (
                    tvSnapshotTimestampTitle.measuredHeight + tvSnapshotTimestampText.measuredHeight +
                        tvSnapshotAppsCountTitle.marginTop +
                        tvSnapshotAppsCountTitle.measuredHeight + tvSnapshotAppsCountText.measuredHeight
                    ).coerceAtLeast(indicatorsHeight) + paddingTop + paddingBottom
            )

        }

        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
            tvSnapshotTimestampTitle.layout(paddingStart, paddingTop)
            tvSnapshotTimestampText.layout(
                tvSnapshotTimestampTitle.left,
                tvSnapshotTimestampTitle.bottom
            )
            arrow.layout(
                tvSnapshotTimestampText.right,
                arrow.toViewVerticalCenter(tvSnapshotTimestampText)
            )
            tvSnapshotAppsCountTitle.layout(
                tvSnapshotTimestampTitle.left,
                tvSnapshotTimestampText.bottom + tvSnapshotAppsCountTitle.marginTop
            )
            tvSnapshotAppsCountText.layout(
                tvSnapshotTimestampTitle.left,
                tvSnapshotAppsCountTitle.bottom
            )
            addedIndicator.layout(paddingEnd, paddingTop, fromRight = true)
            removedIndicator.layout(paddingEnd, addedIndicator.bottom, fromRight = true)
            changedIndicator.layout(paddingEnd, removedIndicator.bottom, fromRight = true)
            movedIndicator.layout(paddingEnd, changedIndicator.bottom, fromRight = true)
        }
    }

}
