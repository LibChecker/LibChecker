package com.absinthe.libchecker.utils.extensions

fun String.toClassDefType(): String {
  val tmp = "L" + replace(".", "/")
  return if (last() == '*') {
    tmp
  } else {
    "$tmp;"
  }
}
