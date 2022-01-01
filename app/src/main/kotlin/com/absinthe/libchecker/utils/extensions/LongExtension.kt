package com.absinthe.libchecker.utils.extensions

import android.content.Context
import android.text.format.Formatter

fun Long.sizeToString(context: Context): String {
  return "${Formatter.formatFileSize(context, this)} ($this Bytes)"
}
