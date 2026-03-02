package com.absinthe.libchecker.features.snapshot.ui.view

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.app.CheckableChipView

class TimeNodeAutoRemoveView(context: Context) : AViewGroup(context) {

  val chip = CheckableChipView(context).also {
    it.layoutParams = MarginLayoutParams(
      FrameLayout.LayoutParams.WRAP_CONTENT,
      FrameLayout.LayoutParams.WRAP_CONTENT
    ).also { lp ->
      lp.setMargins(4.dp, 4.dp, 4.dp, 4.dp)
    }
    val checkedColor = R.color.advanced_menu_item_text_checked.getColor(context)
    val uncheckedColor = R.color.advanced_menu_item_text_not_checked.getColor(context)
    it.textColorPair = checkedColor to uncheckedColor
    it.isChecked = GlobalValues.snapshotAutoRemoveThreshold > 0
  }

  init {
    setPadding(0, 8.dp, 0, 8.dp)
    addView(chip)
    invalidateText()
  }

  fun invalidateText() {
    if (GlobalValues.snapshotAutoRemoveThreshold <= 0) {
      chip.text = context.getString(R.string.album_item_management_snapshot_auto_remove_default_title)
    } else {
      chip.text = context.getString(
        R.string.album_item_management_snapshot_auto_remove_specific_title,
        GlobalValues.snapshotAutoRemoveThreshold
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
