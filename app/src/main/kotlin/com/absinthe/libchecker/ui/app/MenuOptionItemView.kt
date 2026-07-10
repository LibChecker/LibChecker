package com.absinthe.libchecker.ui.app

import android.content.Context
import android.widget.FrameLayout
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.CheckableChipView

class MenuOptionItemView(
  context: Context,
  startMarginDp: Int = 0,
  endMarginDp: Int = 8
) : FrameLayout(context) {

  private val chip = CheckableChipView(context).also {
    it.layoutParams = MarginLayoutParams(
      LayoutParams.WRAP_CONTENT,
      LayoutParams.WRAP_CONTENT
    ).also { layoutParams ->
      layoutParams.setMargins(startMarginDp.dp, 4.dp, endMarginDp.dp, 4.dp)
    }
  }

  init {
    addView(chip)
  }

  fun bind(
    item: MenuOptionItem,
    onCheckedChanged: (Boolean) -> Unit
  ) {
    chip.onCheckedChangeListener = null
    chip.text = context.getString(item.labelRes)
    chip.isChecked = item.isChecked
    chip.onCheckedChangeListener = { _: CheckableChipView, isChecked: Boolean ->
      onCheckedChanged(isChecked)
    }
  }
}
