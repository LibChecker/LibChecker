package com.absinthe.libchecker.domain.snapshot.timenode.ui.view

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import com.absinthe.libchecker.R
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.app.CheckableChipView

class TimeNodeAutoRemoveView(context: Context) : AViewGroup(context) {

  private var autoRemoveThreshold = -1
  private var isBinding = false
  private var onCheckedChange: (Boolean) -> Unit = {}

  private val chip = CheckableChipView(context).also {
    it.layoutParams = MarginLayoutParams(
      FrameLayout.LayoutParams.WRAP_CONTENT,
      FrameLayout.LayoutParams.WRAP_CONTENT
    ).also { lp ->
      lp.setMargins(4.dp, 4.dp, 4.dp, 4.dp)
    }
  }

  init {
    setPadding(0, 8.dp, 0, 8.dp)
    addView(chip)
    invalidateText()
    chip.onCheckedChangeListener = { _, checked ->
      if (!isBinding) {
        onCheckedChange(checked)
      }
    }
  }

  fun bind(
    autoRemoveThreshold: Int,
    onCheckedChange: (Boolean) -> Unit
  ) {
    this.autoRemoveThreshold = autoRemoveThreshold
    this.onCheckedChange = onCheckedChange
    isBinding = true
    chip.isChecked = autoRemoveThreshold > 0
    isBinding = false
    invalidateText()
  }

  private fun invalidateText() {
    if (autoRemoveThreshold <= 0) {
      chip.text = context.getString(R.string.album_item_management_snapshot_auto_remove_default_title)
    } else {
      chip.text = context.getString(
        R.string.album_item_management_snapshot_auto_remove_specific_title,
        autoRemoveThreshold
      )
    }
  }

  override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams {
    return ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    chip.autoMeasure()
    setMeasuredDimension(
      measuredWidth,
      paddingTop + paddingBottom + chip.measuredHeight
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    chip.layout(paddingStart, chip.toVerticalCenter(this))
  }
}
