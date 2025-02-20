package com.absinthe.libchecker.utils

import android.os.SystemProperties

object DeviceUtils {
  fun isOppoDomestic(): Boolean {
    return SystemProperties.get("ro.oplus.image.system_ext.area") == "domestic"
  }
}
