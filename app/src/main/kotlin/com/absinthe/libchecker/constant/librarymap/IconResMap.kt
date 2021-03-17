package com.absinthe.libchecker.constant.librarymap

import android.util.SparseIntArray
import com.absinthe.libchecker.R

object IconResMap {
    private val MAP = SparseIntArray()
    private val SINGLE_COLOR_ICON_SET: Set<Int>

    init {
        MAP.apply {
            put(-1, R.drawable.ic_sdk_placeholder)
            put(0, R.drawable.ic_lib_360)
            put(1, R.drawable.ic_lib_airbnb)
            put(2, R.drawable.ic_lib_ali_security)
            put(3, R.drawable.ic_lib_alibaba)
            put(4, R.drawable.ic_lib_alipay)
            put(5, R.drawable.ic_lib_aliyun)
            put(6, R.drawable.ic_lib_amap)
            put(7, R.drawable.ic_lib_android)
            put(8, R.drawable.ic_lib_appauth)
            put(9, R.drawable.ic_lib_baidu)
            put(10, R.drawable.ic_lib_bilibili)
            put(11, R.drawable.ic_lib_bugly)
            put(12, R.drawable.ic_lib_bytedance)
            put(13, R.drawable.ic_lib_chromium)
            put(14, R.drawable.ic_lib_cmb)
            put(15, R.drawable.ic_lib_cpp)
            put(16, R.drawable.ic_lib_didi)
            put(17, R.drawable.ic_lib_evernote)
            put(18, R.drawable.ic_lib_facebook)
            put(19, R.drawable.ic_lib_firebase)
            put(20, R.drawable.ic_lib_flutter)
            put(21, R.drawable.ic_lib_golang)
            put(22, R.drawable.ic_lib_google)
            put(23, R.drawable.ic_lib_google_analytics)
            put(24, R.drawable.ic_lib_google_arcore)
            put(25, R.drawable.ic_lib_hapjs)
            put(26, R.drawable.ic_lib_huawei)
            put(27, R.drawable.ic_lib_iqiyi)
            put(28, R.drawable.ic_lib_jetpack)
            put(29, R.drawable.ic_lib_jpush)
            put(30, R.drawable.ic_lib_jverification)
            put(31, R.drawable.ic_lib_kuaishou)
            put(32, R.drawable.ic_lib_lua)
            put(33, R.drawable.ic_lib_material)
            put(34, R.drawable.ic_lib_meizu)
            put(35, R.drawable.ic_lib_microsoft)
            put(36, R.drawable.ic_lib_netease)
            put(37, R.drawable.ic_lib_opencv)
            put(38, R.drawable.ic_lib_oppo)
            put(39, R.drawable.ic_lib_play_store)
            put(40, R.drawable.ic_lib_qiniu)
            put(41, R.drawable.ic_lib_react)
            put(42, R.drawable.ic_lib_realm)
            put(43, R.drawable.ic_lib_rongyun)
            put(44, R.drawable.ic_lib_sensors)
            put(45, R.drawable.ic_lib_shizuku)
            put(46, R.drawable.ic_lib_sql)
            put(47, R.drawable.ic_lib_square)
            put(48, R.drawable.ic_lib_tencent)
            put(49, R.drawable.ic_lib_tencent_ad)
            put(50, R.drawable.ic_lib_tencent_cloud)
            put(51, R.drawable.ic_lib_tencent_map)
            put(52, R.drawable.ic_lib_tensorflow)
            put(53, R.drawable.ic_lib_umeng)
            put(54, R.drawable.ic_lib_unionpay)
            put(55, R.drawable.ic_lib_unity)
            put(56, R.drawable.ic_lib_unreal_engine)
            put(57, R.drawable.ic_lib_vivo)
            put(58, R.drawable.ic_lib_webrtc)
            put(59, R.drawable.ic_lib_weibo)
            put(60, R.drawable.ic_lib_xamarin)
            put(61, R.drawable.ic_lib_xiaoai)
            put(62, R.drawable.ic_lib_xiaomi)
            put(63, R.drawable.ic_lib_xunfei)
            put(64, R.drawable.ic_lib_zhihu)
            put(65, R.drawable.ic_kotlin_logo)
            put(66, R.drawable.ic_telegram)
            put(67, R.drawable.ic_lib_ffmpeg)
            put(68, R.drawable.ic_lib_vlc)
        }

        SINGLE_COLOR_ICON_SET = setOf(
            -1, 1, 2, 3, 4, 5, 6, 9, 10, 11, 13, 14, 15, 16, 17,
            25, 27, 30, 31, 40, 42, 43, 44, 46, 47, 48, 50, 51,
            52, 53, 54, 55, 56, 59, 63, 64, 66, 67
        )
    }

    fun getResIndex(res: Int) = MAP.keyAt(MAP.indexOfValue(res))

    fun getIconRes(index: Int) = MAP.get(index, R.drawable.ic_sdk_placeholder)

    fun isSingleColorIcon(index: Int) = SINGLE_COLOR_ICON_SET.contains(index)
}