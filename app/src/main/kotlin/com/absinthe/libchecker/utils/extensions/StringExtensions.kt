package com.absinthe.libchecker.utils.extensions

import androidx.core.text.isDigitsOnly

fun String.toClassDefType(): String {
  val tmp = "L" + replace(".", "/")
  return if (last() == '*') {
    tmp
  } else {
    "$tmp;"
  }
}

fun String.maybeResourceId(): Boolean {
  return this.isNotBlank() && this.isDigitsOnly() && this.toLongOrNull() != null
}

fun String.removeNonDigits(): String {
  return this.replace(Regex("\\D"), "")
}
