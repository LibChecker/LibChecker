package com.absinthe.libchecker.features.snapshot.ui.view

import android.content.Context
import android.widget.FrameLayout
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.CheckableChipView

class SnapshotMenuItemView(context: Context) : FrameLayout(context) {
  val chip = CheckableChipView(context).also {
    it.layoutParams = MarginLayoutParams(
      LayoutParams.WRAP_CONTENT,
      LayoutParams.WRAP_CONTENT
    ).also { lp ->
      lp.setMargins(0, 4.dp, 8.dp, 4.dp)
    }
  }

  private var onCheckedChangeCallback: ((isChecked: Boolean) -> Unit)? = null

  init {
    addView(chip)
  }

  fun setOption(labelRes: Int, option: Int, currentOptions: Int) {
    chip.apply {
      text = context.getString(labelRes)
      isChecked = (currentOptions and option) > 0
      onCheckedChangeListener = { _: CheckableChipView, isChecked: Boolean ->
        onCheckedChangeCallback?.invoke(isChecked)
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
