package com.absinthe.libchecker.utils.extensions

import java.io.File
import java.security.MessageDigest
import okio.ByteString
import okio.ByteString.Companion.toByteString

fun ByteArray.sha1(separator: CharSequence = ""): String = toByteString().sha1().toHex(separator)

fun ByteArray.sha256(separator: CharSequence = ""): String = toByteString().sha256().toHex(separator)

fun ByteArray.md5(separator: CharSequence = ""): String = toByteString().md5().toHex(separator)

fun File.md5(separator: CharSequence = ""): String {
  val md = MessageDigest.getInstance("MD5")
  inputStream().use { input ->
    val buffer = ByteArray(8192)
    var bytesRead: Int
    while (input.read(buffer).also { bytesRead = it } != -1) {
      md.update(buffer, 0, bytesRead)
    }
  }
  return md.digest().toHexString(separator)
}

fun ByteArray.toHexString(separator: CharSequence = ""): String = joinToString(separator) {
  it.toInt().and(0xff).toString(16).padStart(2, '0')
}.uppercase()

private fun ByteString.toHex(separator: CharSequence) = toByteArray().toHexString(separator)
