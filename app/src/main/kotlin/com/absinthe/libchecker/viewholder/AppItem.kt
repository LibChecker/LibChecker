package com.absinthe.libchecker.viewholder

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable

const val ARMV8 = 0
const val ARMV7 = 1
const val ARMV5 = 2
const val NO_LIBS = 3

const val ARMV8_STRING = "ARMv8"
const val ARMV7_STRING = "ARMv7"
const val ARMV5_STRING = "ARMv5"

class AppItem {
    var icon: Drawable = ColorDrawable(Color.TRANSPARENT)
    var appName: String = ""
    var packageName: String = ""
    var versionName: String = ""
    var abi: Int = NO_LIBS
    var isSystem: Boolean = false
}