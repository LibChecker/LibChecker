package com.absinthe.libchecker.utils.extensions

import android.content.Context
import android.text.format.Formatter
import android.text.format.FormatterHidden
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.options.SnapshotOptions
import java.io.File

fun String.isTempApk(): Boolean {
  return endsWith("${File.separator}${Constants.TEMP_PACKAGE}")
}

val supportIECUnit by lazy {
  runCatching { FormatterHidden.formatFileSize(LibCheckerApp.app, 0, FormatterHidden.FLAG_IEC_UNITS) }.isSuccess
}

fun Long.sizeToString(context: Context, showBytes: Boolean = true): String {
  val formattedSize = if (supportIECUnit) {
    val flag =
      if ((GlobalValues.snapshotOptions and SnapshotOptions.USE_IEC_UNITS) > 0) FormatterHidden.FLAG_IEC_UNITS else FormatterHidden.FLAG_SI_UNITS
    FormatterHidden.formatFileSize(context, this, flag)
  } else {
    Formatter.formatFileSize(context, this)
  }
  return if (showBytes) "$formattedSize ($this Bytes)" else formattedSize
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
