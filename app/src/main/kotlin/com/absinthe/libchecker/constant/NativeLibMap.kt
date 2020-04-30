package com.absinthe.libchecker.constant

import com.absinthe.libchecker.R

object NativeLibMap {
    val MAP: HashMap<String, LibChip> = hashMapOf(
        Pair("libflutter.so", LibChip(R.drawable.ic_lib_flutter, "Flutter")),
        Pair("libapp.so", LibChip(R.drawable.ic_lib_flutter, "Flutter")),
        Pair("libBugly.so", LibChip(R.drawable.ic_lib_bugly, "Bugly")),
        Pair("libBugly-rqd.so", LibChip(R.drawable.ic_lib_bugly, "Bugly")),
        Pair("libtpnsSecurity.so", LibChip(R.drawable.ic_lib_tencent, "信鸽推送")),
        Pair("libxguardian.so", LibChip(R.drawable.ic_lib_tencent, "信鸽推送")),
        Pair("libijkplayer.so", LibChip(R.drawable.ic_lib_bilibili, "IJKPlayer")),
        Pair("libijksdl.so", LibChip(R.drawable.ic_lib_bilibili, "IJKPlayer")),
        Pair("libijkffmpeg.so", LibChip(R.drawable.ic_lib_bilibili, "IJKPlayer")),
        Pair("libreactnativejni.so", LibChip(R.drawable.ic_lib_react, "React Native")),
        Pair("libjiagu.so", LibChip(R.drawable.ic_lib_360, "360 加固")),
        Pair("libc++_shared.so", LibChip(R.drawable.ic_lib_cpp, "C++ 共享库")),
        Pair("libwind.so", LibChip(R.drawable.ic_lib_weibo, "微博 SDK")),
        Pair("libutility.so", LibChip(R.drawable.ic_lib_weibo, "微博 SDK")),
        Pair("libweibosdkcore.so", LibChip(R.drawable.ic_lib_weibo, "微博 SDK")),
        Pair("libmsc.so", LibChip(R.drawable.ic_lib_xunfei, "讯飞 SDK")),
        Pair("libmmkv.so", LibChip(R.drawable.ic_lib_tencent, "MMKV")),
        Pair("libluajava.so", LibChip(R.drawable.ic_lib_lua, "AndroLua")),
        Pair("libRSSupport.so", LibChip(R.drawable.ic_android_outline, "RenderScript")),
        Pair("librsjni.so", LibChip(R.drawable.ic_android_outline, "RenderScript")),
        Pair("libweexcore.so", LibChip(R.drawable.ic_lib_alibaba, "Weex")),
        Pair("libWeexEagle.so", LibChip(R.drawable.ic_lib_alibaba, "Weex")),
        Pair("libweexjst.so", LibChip(R.drawable.ic_lib_alibaba, "Weex")),
        Pair("libweexjss.so", LibChip(R.drawable.ic_lib_alibaba, "Weex")),
        Pair("libweexjsb.so", LibChip(R.drawable.ic_lib_alibaba, "Weex")),
        Pair("libweexjssr.so", LibChip(R.drawable.ic_lib_alibaba, "Weex")),
        Pair("libWTF.so", LibChip(R.drawable.ic_lib_alibaba, "Weex")),
        Pair("libtencentloc.so", LibChip(R.drawable.ic_lib_tencent_map, "腾讯地图 SDK")),
        Pair("libyuv.so", LibChip(R.drawable.ic_lib_google, "libYUV")),
        Pair("libunity.so", LibChip(R.drawable.ic_lib_unity, "Unity")),
        Pair("libmain.so", LibChip(R.drawable.ic_lib_unity, "Unity")),
        Pair("libmono.so", LibChip(R.drawable.ic_lib_unity, "Unity")),
        Pair("libyoga.so", LibChip(R.drawable.ic_lib_facebook, "Yoga")),
        Pair("libglog.so", LibChip(R.drawable.ic_lib_google, "glog")),
        Pair("librealm-jni.so", LibChip(R.drawable.ic_lib_realm, "realm"))
    )

    data class LibChip(val iconRes: Int, val name: String)
}