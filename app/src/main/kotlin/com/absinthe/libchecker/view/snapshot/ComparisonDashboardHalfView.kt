package com.absinthe.libchecker.view.snapshot

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginTop
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup

class ComparisonDashboardHalfView(context: Context, attributeSet: AttributeSet) :
  AViewGroup(context, attributeSet) {

  private val tvSnapshotTimestampTitle =
    AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerif)).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      text = context.getString(R.string.snapshot_current_timestamp)
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
    }

  val tvSnapshotTimestampText =
    AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerifBlack)).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextColor(context.getColorByAttr(R.attr.colorOnSurface))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
      setBackgroundResource(context.getResourceIdByAttr(android.R.attr.selectableItemBackgroundBorderless))
      text = context.getString(R.string.album_click_to_choose)
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
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
    }

  val tvSnapshotAppsCountText =
    AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerifBlack)).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextColor(context.getColorByAttr(R.attr.colorOnSurface))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
      text = "0"
    }

  init {
    addView(tvSnapshotTimestampTitle)
    addView(tvSnapshotTimestampText)
    addView(tvSnapshotAppsCountTitle)
    addView(tvSnapshotAppsCountText)
  }

  var horizontalGravity: Int = Gravity.START
    set(value) {
      field = value
      tvSnapshotTimestampTitle.gravity = field
      tvSnapshotTimestampText.gravity = field
      tvSnapshotAppsCountTitle.gravity = field
      tvSnapshotAppsCountText.gravity = field
    }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)

    val textWidth = measuredWidth - paddingStart - paddingEnd
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
      tvSnapshotTimestampTitle.measuredHeight + tvSnapshotTimestampText.measuredHeight +
        tvSnapshotAppsCountTitle.marginTop +
        tvSnapshotAppsCountTitle.measuredHeight + tvSnapshotAppsCountText.measuredHeight +
        paddingTop + paddingBottom
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    tvSnapshotTimestampTitle.layout(
      paddingStart,
      paddingTop,
      fromRight = horizontalGravity == Gravity.END
    )
    tvSnapshotTimestampText.layout(
      tvSnapshotTimestampTitle.left,
      tvSnapshotTimestampTitle.bottom,
      fromRight = horizontalGravity == Gravity.END
    )
    tvSnapshotAppsCountTitle.layout(
      tvSnapshotTimestampTitle.left,
      tvSnapshotTimestampText.bottom + tvSnapshotAppsCountTitle.marginTop,
      fromRight = horizontalGravity == Gravity.END
    )
    tvSnapshotAppsCountText.layout(
      tvSnapshotTimestampTitle.left,
      tvSnapshotAppsCountTitle.bottom,
      fromRight = horizontalGravity == Gravity.END
    )
  }
}
