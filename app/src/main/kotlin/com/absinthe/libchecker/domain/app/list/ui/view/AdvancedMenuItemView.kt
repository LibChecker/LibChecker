package com.absinthe.libchecker.domain.app.list.ui.view

import android.content.Context
import android.widget.FrameLayout
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.CheckableChipView

class AdvancedMenuItemView(context: Context) : FrameLayout(context) {
  val chip = CheckableChipView(context).also {
    it.layoutParams = MarginLayoutParams(
      LayoutParams.WRAP_CONTENT,
      LayoutParams.WRAP_CONTENT
    ).also { lp ->
      lp.setMargins(0, 4.dp, 8.dp, 4.dp)
    }
  }

  init {
    addView(chip)
  }

  fun setOption(labelRes: Int, isChecked: Boolean, onCheckedChanged: (Boolean) -> Unit) {
    chip.apply {
      text = context.getString(labelRes)
      this.isChecked = isChecked
      onCheckedChangeListener = { _: CheckableChipView, isChecked: Boolean ->
        onCheckedChanged(isChecked)
      }
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    chip.onCheckedChangeListener = null
  }
}
