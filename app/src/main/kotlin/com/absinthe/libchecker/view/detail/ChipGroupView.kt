package com.absinthe.libchecker.view.detail

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.ContextThemeWrapper
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.dp
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class ChipGroupView(context: Context) : ChipGroup(context) {

  init {
    clipToPadding = false
    setPadding(0, 4.dp, 0, 4.dp)
    chipSpacingHorizontal = 16.dp
  }

  fun addChip(icon: Drawable, text: String, clickAction: () -> Unit) {
    addView(
      Chip(ContextThemeWrapper(context, R.style.App_LibChip)).also {
        it.chipIcon = icon
        it.text = text
        it.setOnClickListener { clickAction() }
      }
    )
  }
}
