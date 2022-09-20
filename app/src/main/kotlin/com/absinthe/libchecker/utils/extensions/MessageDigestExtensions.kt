package com.absinthe.libchecker.utils.extensions

import java.io.File
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString
import okio.HashingSource
import okio.blackholeSink
import okio.buffer
import okio.source

fun String.sha1(separator: CharSequence = "") = encodeUtf8().sha1().toHex(separator)

fun String.sha256(separator: CharSequence = "") = encodeUtf8().sha256().toHex(separator)

fun String.sha512(separator: CharSequence = "") = encodeUtf8().sha512().toHex(separator)

fun String.md5(separator: CharSequence = "") = encodeUtf8().md5().toHex(separator)

fun ByteArray.sha1(separator: CharSequence = "") = toByteString().sha1().toHex(separator)

fun ByteArray.sha256(separator: CharSequence = "") = toByteString().sha256().toHex(separator)

fun ByteArray.sha512(separator: CharSequence = "") = toByteString().sha512().toHex(separator)

fun ByteArray.md5(separator: CharSequence = "") = toByteString().md5().toHex(separator)

fun File.sha1(separator: CharSequence = "") = HashingSource.sha1(source()).hash(separator)

fun File.sha256(separator: CharSequence = "") = HashingSource.sha256(source()).hash(separator)

fun File.sha512(separator: CharSequence = "") = HashingSource.sha512(source()).hash(separator)

fun File.md5(separator: CharSequence = "") = HashingSource.md5(source()).hash(separator)

private fun HashingSource.hash(separator: CharSequence): String {
  buffer().readAll(blackholeSink())
  return hash.toHex(separator)
}

private fun ByteString.toHex(separator: CharSequence) = toByteArray().toHexString(separator)
