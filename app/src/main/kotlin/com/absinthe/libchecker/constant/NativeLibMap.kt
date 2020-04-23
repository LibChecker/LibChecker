package com.absinthe.libchecker.constant

import com.absinthe.libchecker.R

object NativeLibMap {
    val MAP: HashMap<String, LibChip> = hashMapOf(
        Pair("libflutter.so", LibChip(R.drawable.ic_flutter, "Flutter")),
        Pair("libBugly.so", LibChip(R.drawable.ic_bugly, "Bugly")),
        Pair("libtpnsSecurity.so", LibChip(R.drawable.ic_tencent, "Xinge")),
        Pair("libxguardian.so", LibChip(R.drawable.ic_tencent, "Xinge")),
        Pair("libijkplayer.so", LibChip(R.drawable.ic_bilibili, "IJKPlayer")),
        Pair("libreactnativejni.so", LibChip(R.drawable.ic_react, "React Native"))
    )

    data class LibChip(val iconRes: Int, val name: String)
}