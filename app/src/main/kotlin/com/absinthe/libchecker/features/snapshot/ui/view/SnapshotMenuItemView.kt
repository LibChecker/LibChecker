package com.absinthe.libchecker.features.snapshot.ui.view

import android.content.Context
import android.widget.FrameLayout
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libchecker.view.app.CheckableChipView

class SnapshotMenuItemView(context: Context) : FrameLayout(context) {
  val chip = CheckableChipView(context).also {
    it.layoutParams = MarginLayoutParams(
      LayoutParams.WRAP_CONTENT,
      LayoutParams.WRAP_CONTENT
    ).also { lp ->
      lp.setMargins(4.dp, 4.dp, 4.dp, 4.dp)
    }
    val checkedColor = R.color.advanced_menu_item_text_checked.getColor(context)
    val uncheckedColor = R.color.advanced_menu_item_text_not_checked.getColor(context)
    it.textColorPair = checkedColor to uncheckedColor
  }

  private var onCheckedChangeCallback: ((isChecked: Boolean) -> Unit)? = null

  init {
    addView(chip)
  }

  fun setOption(labelRes: Int, option: Int) {
    chip.apply {
      text = context.getString(labelRes)
      isChecked = (GlobalValues.snapshotOptions and option) > 0
      onCheckedChangeListener = { _: CheckableChipView, isChecked: Boolean ->
        val newOptions = if (isChecked) {
          GlobalValues.snapshotOptions or option
        } else {
          GlobalValues.snapshotOptions and option.inv()
        }
        GlobalValues.snapshotOptions = newOptions
        onCheckedChangeCallback?.invoke(isChecked)
        Telemetry.recordEvent("snapshot_advanced_item_option_changed", mapOf("option" to text, "isChecked" to isChecked))
      }
    }
  }

  fun setChecked(isChecked: Boolean) {
    chip.setCheckedAnimated(isChecked, null)
  }

  fun setOnCheckedChangeCallback(action: (isChecked: Boolean) -> Unit) {
    onCheckedChangeCallback = action
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    chip.onCheckedChangeListener = null
    onCheckedChangeCallback = null
  }
}
