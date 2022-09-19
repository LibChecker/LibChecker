package com.absinthe.libchecker.utils.extensions

fun ByteArray.toHexString(separator: CharSequence = "") = joinToString(separator) {
  it.toInt().and(0xff).toString(16).padStart(2, '0')
}
