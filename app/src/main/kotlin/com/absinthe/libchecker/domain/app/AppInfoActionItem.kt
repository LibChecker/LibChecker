package com.absinthe.libchecker.domain.app

import android.content.Intent
import android.graphics.drawable.Drawable

data class AppInfoActionItem(
  val packageName: String,
  val label: CharSequence,
  val icon: Drawable?,
  val intent: Intent
)
