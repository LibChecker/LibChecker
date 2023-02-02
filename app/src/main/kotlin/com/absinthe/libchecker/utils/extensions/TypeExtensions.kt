package com.absinthe.libchecker.utils.extensions

import android.content.Context
import android.text.format.Formatter
import com.absinthe.libchecker.constant.Constants
import java.io.File

fun String.isTempApk(): Boolean {
  return endsWith("${File.separator}${Constants.TEMP_PACKAGE}")
}

fun Long.sizeToString(context: Context): String {
  return "${Formatter.formatFileSize(context, this)} ($this Bytes)"
}

fun ByteArray.toHex(): String {
  return buildString {
    this@toHex.forEach {
      var hex = Integer.toHexString(it.toInt() and 0xFF)
      if (hex.length == 1) {
        hex = "0$hex"
      }
      append(hex.lowercase())
    }
  }
}
