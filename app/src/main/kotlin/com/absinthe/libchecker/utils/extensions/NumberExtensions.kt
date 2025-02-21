package com.absinthe.libchecker.utils.extensions

fun Float.toPercentage(): String {
  return "${(this * 100)}%"
}

fun Int.roundUpToNearestTen(): Int {
  return if (this % 10 == 0) {
    this
  } else {
    this + 10 - this % 10
  }
}

fun Long?.orZero(): Long {
  return this ?: 0L
}
