package com.absinthe.libchecker.viewholder

import android.graphics.drawable.Drawable

const val ARMV8 = 0
const val ARMV7 = 1
const val ARMV5 = 2
const val NO_LIBS = 3

const val ARMV8_STRING = "ARMv8"
const val ARMV7_STRING = "ARMv7"
const val ARMV5_STRING = "ARMv5"
const val NO_LIBS_STRING = "No libs"

class AppItem(var icon: Drawable, var appName: String, var abi: Int)