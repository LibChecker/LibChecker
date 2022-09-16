package com.absinthe.libchecker.utils

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object MessageDigestUtils {

  const val UTF_8 = "UTF-8"

  const val MD5 = "MD5"
  const val SHA_1 = "SHA-1"
  const val SHA_256 = "SHA-256"
  const val SHA_512 = "SHA-512"
  const val CRC_32 = "CRC-32"

  const val SEPARATOR_EMPTY = ""
  const val SEPARATOR_COLON = ":"

  fun sha1(string: String, separator: CharSequence = SEPARATOR_EMPTY) =
    digest(SHA_1, string, separator)

  fun sha256(string: String, separator: CharSequence = SEPARATOR_EMPTY) =
    digest(SHA_256, string, separator)

  fun sha512(string: String, separator: CharSequence = SEPARATOR_EMPTY) =
    digest(SHA_512, string, separator)

  fun crc32(string: String, separator: CharSequence = SEPARATOR_EMPTY) =
    digest(CRC_32, string, separator)

  fun md5(string: String, separator: CharSequence = SEPARATOR_EMPTY) =
    digest(MD5, string, separator)

  fun sha1(bytes: ByteArray, separator: CharSequence = SEPARATOR_EMPTY) =
    digest(SHA_1, bytes, separator)

  fun sha256(bytes: ByteArray, separator: CharSequence = SEPARATOR_EMPTY) =
    digest(SHA_256, bytes, separator)

  fun sha512(bytes: ByteArray, separator: CharSequence = SEPARATOR_EMPTY) =
    digest(SHA_512, bytes, separator)

  fun crc32(bytes: ByteArray, separator: CharSequence = SEPARATOR_EMPTY) =
    digest(CRC_32, bytes, separator)

  fun md5(bytes: ByteArray, separator: CharSequence = SEPARATOR_EMPTY) =
    digest(MD5, bytes, separator)

  fun digest(
    algorithm: String,
    string: String,
    separator: CharSequence = SEPARATOR_EMPTY
  ) = runCatching {
    digest(algorithm, string.toByteArray(charset(UTF_8)), separator)
  }.getOrDefault("")

  fun digest(
    algorithm: String,
    bytes: ByteArray,
    separator: CharSequence = SEPARATOR_EMPTY
  ) = runCatching {
    val md = MessageDigest.getInstance(algorithm)
    md.digest(bytes).toHexString(separator)
  }.getOrDefault("")

  fun sha1OfFile(filePath: String, separator: CharSequence = SEPARATOR_EMPTY): String {
    return digest(SHA_1, File(filePath), separator)
  }

  fun sha256OfFile(filePath: String, separator: CharSequence = SEPARATOR_EMPTY): String {
    return digest(SHA_256, File(filePath), separator)
  }

  fun sha512OfFile(filePath: String, separator: CharSequence = SEPARATOR_EMPTY): String {
    return digest(SHA_512, File(filePath), separator)
  }

  fun crc32OfFile(filePath: String, separator: CharSequence = SEPARATOR_EMPTY): String {
    return digest(CRC_32, File(filePath), separator)
  }

  fun md5OfFile(filePath: String, separator: CharSequence = SEPARATOR_EMPTY): String {
    return md5(File(filePath), separator)
  }

  fun sha1(file: File, separator: CharSequence = SEPARATOR_EMPTY): String {
    return digest(SHA_1, file, separator)
  }

  fun sha256(file: File, separator: CharSequence = SEPARATOR_EMPTY): String {
    return digest(SHA_256, file, separator)
  }

  fun sha512(file: File, separator: CharSequence = SEPARATOR_EMPTY): String {
    return digest(SHA_512, file, separator)
  }

  fun crc32(file: File, separator: CharSequence = SEPARATOR_EMPTY): String {
    return digest(CRC_32, file, separator)
  }

  fun md5(file: File, separator: CharSequence = SEPARATOR_EMPTY): String {
    return digest(MD5, file, separator)
  }

  fun digest(
    algorithm: String,
    file: File,
    separator: CharSequence = SEPARATOR_EMPTY
  ) = runCatching {
    FileInputStream(file).use { inputStream ->
      ByteArrayOutputStream().use { outputStream ->
        var buffer = ByteArray(1024 * 2)
        var readLength = 0
        val md5 = MessageDigest.getInstance(algorithm)
        while (inputStream.read(buffer).also { readLength = it } > 0) {
          outputStream.write(buffer, 0, readLength)
        }
        buffer = outputStream.toByteArray()
        md5.update(buffer, 0, buffer.size)
        md5.digest().toHexString(separator)
      }
    }
  }.getOrDefault("")
}
