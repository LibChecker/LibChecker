package com.absinthe.libchecker.utils.extensions

import com.absinthe.libchecker.constant.Constants
import java.io.File

fun String.isTempApk(): Boolean {
  return endsWith("${File.separator}${Constants.TEMP_PACKAGE}")
}
