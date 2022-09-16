package com.absinthe.libchecker.utils

fun ByteArray.toHexString(separator: CharSequence = "") = joinToString(separator) {
  it.toInt().and(0xff).toString(16).padStart(2, '0')
}
