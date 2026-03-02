package com.absinthe.libchecker.utils.extensions

import java.io.File
import okio.ByteString
import okio.ByteString.Companion.toByteString

fun ByteArray.sha1(separator: CharSequence = ""): String = toByteString().sha1().toHex(separator)

fun ByteArray.sha256(separator: CharSequence = ""): String = toByteString().sha256().toHex(separator)

fun ByteArray.md5(separator: CharSequence = ""): String = toByteString().md5().toHex(separator)

fun ByteArray.toHexString(separator: CharSequence = ""): String = joinToString(separator) {
  it.toInt().and(0xff).toString(16).padStart(2, '0')
}.uppercase()

fun File.md5(): String = readBytes().md5()

private fun ByteString.toHex(separator: CharSequence) = toByteArray().toHexString(separator)
