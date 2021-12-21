package com.absinthe.libchecker.view.detail

import android.content.Context
import android.graphics.drawable.Drawable
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class ChipGroupView(context: Context) : ChipGroup(context) {

  init {
    clipToPadding = false
    clipChildren = false
    setPadding(0, 4.dp, 0, 4.dp)
    chipSpacingHorizontal = 16.dp
  }

  fun addChip(icon: Drawable, text: String, clickAction: () -> Unit) {
    addView(
      Chip(context).also {
        it.chipIcon = icon
        it.text = text
        it.setOnClickListener { view ->
          if (AntiShakeUtils.isInvalidClick(view)) {
            return@setOnClickListener
          }
          clickAction()
        }
      }
    )
  }
}
