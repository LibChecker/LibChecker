package com.absinthe.libchecker.utils

import android.os.SystemProperties

object DeviceUtils {
  fun isOppoDomestic(): Boolean {
    return SystemProperties.get("ro.oplus.image.system_ext.area") == "domestic"
  }

  val isSupportBackgroundBlur by lazy {
    SystemProperties.get("ro.surface_flinger.supports_background_blur") == "1"
  }
}
