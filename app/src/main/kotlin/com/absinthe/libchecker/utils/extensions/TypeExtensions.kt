@file:Suppress("SoonBlockedPrivateApi")

package com.absinthe.libchecker.utils.extensions

import android.content.Context
import android.text.format.Formatter
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.options.SnapshotOptions
import java.io.File

fun String.isTempApk(): Boolean {
  return endsWith("${File.separator}${Constants.TEMP_PACKAGE}")
}

private val FLAG_IEC_UNITS by lazy {
  Formatter::class.java.getDeclaredField("FLAG_IEC_UNITS").getInt(null)
}
private val FLAG_SI_UNITS by lazy {
  Formatter::class.java.getDeclaredField("FLAG_SI_UNITS").getInt(null)
}
private val formatFileSizeMethod by lazy {
  Formatter::class.java.getDeclaredMethod(
    "formatFileSize",
    Context::class.java,
    Long::class.java,
    Int::class.java
  )
}

fun Long.sizeToString(context: Context, showBytes: Boolean = true): String {
  val flag = if ((GlobalValues.snapshotOptions and SnapshotOptions.USE_IEC_UNITS) > 0) FLAG_IEC_UNITS else FLAG_SI_UNITS
  val formattedSize = formatFileSizeMethod.invoke(null, context, this, flag) as String
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
