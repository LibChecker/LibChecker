package com.absinthe.libchecker.utils.extensions

fun Float.toPercentage(): String {
  return "${(this * 100)}%"
}
