package com.absinthe.libchecker.utils.extensions

import java.io.File
import okio.ByteString
import okio.ByteString.Companion.toByteString
import okio.HashingSink
import okio.blackholeSink
import okio.buffer
import okio.source

fun ByteArray.sha1(separator: CharSequence = ""): String = toByteString().sha1().toHex(separator)

fun ByteArray.sha256(separator: CharSequence = ""): String = toByteString().sha256().toHex(separator)

fun ByteArray.md5(separator: CharSequence = ""): String = toByteString().md5().toHex(separator)

fun File.md5(separator: CharSequence = ""): String {
  val sink = HashingSink.md5(blackholeSink())
  source().buffer().use { it.readAll(sink) }
  return sink.hash.toHex(separator)
}

fun ByteArray.toHexString(separator: CharSequence = ""): String = toHexString(
  HexFormat { bytes.byteSeparator = separator.toString() }
).uppercase()

private fun ByteString.toHex(separator: CharSequence) = toByteArray().toHexString(separator)
