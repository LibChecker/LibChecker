package com.absinthe.libchecker.constant

import com.absinthe.libchecker.R

object NativeLibMap {
    val MAP: HashMap<String, Int> = hashMapOf(
        Pair("libflutter.so", R.drawable.lib_flutter),
        Pair("libBugly.so", R.drawable.lib_bugly),
        Pair("libtpnsSecurity.so", R.drawable.lib_xgpush),
        Pair("libxguardian.so", R.drawable.lib_xgpush)
    )
}