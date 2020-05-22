package com.absinthe.libchecker.view

import android.content.res.Resources

object ViewKtx {
    val Number.dp: Int get() = (toInt() * Resources.getSystem().displayMetrics.density).toInt()
}