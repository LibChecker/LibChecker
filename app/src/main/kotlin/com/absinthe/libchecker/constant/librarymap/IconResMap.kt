package com.absinthe.libchecker.constant.librarymap

import com.absinthe.libchecker.R

object IconResMap {
    private val MAP = hashMapOf(
        -1 to R.drawable.ic_sdk_placeholder,
        0 to R.drawable.ic_lib_360,
        1 to R.drawable.ic_lib_airbnb,
        2 to R.drawable.ic_lib_ali_security,
        3 to R.drawable.ic_lib_alibaba,
        4 to R.drawable.ic_lib_alipay,
        5 to R.drawable.ic_lib_aliyun,
        6 to R.drawable.ic_lib_amap,
        7 to R.drawable.ic_lib_android,
        8 to R.drawable.ic_lib_appauth,
        9 to R.drawable.ic_lib_baidu,
        10 to R.drawable.ic_lib_bilibili,
        11 to R.drawable.ic_lib_bugly,
        12 to R.drawable.ic_lib_bytedance,
        13 to R.drawable.ic_lib_chromium,
        14 to R.drawable.ic_lib_cmb,
        15 to R.drawable.ic_lib_cpp,
        16 to R.drawable.ic_lib_didi,
        17 to R.drawable.ic_lib_evernote,
        18 to R.drawable.ic_lib_facebook,
        19 to R.drawable.ic_lib_firebase,
        20 to R.drawable.ic_lib_flutter,
        21 to R.drawable.ic_lib_golang,
        22 to R.drawable.ic_lib_google,
        23 to R.drawable.ic_lib_google_analytics,
        24 to R.drawable.ic_lib_google_arcore,
        25 to R.drawable.ic_lib_hapjs,
        26 to R.drawable.ic_lib_huawei,
        27 to R.drawable.ic_lib_iqiyi,
        28 to R.drawable.ic_lib_jetpack,
        29 to R.drawable.ic_lib_jpush,
        30 to R.drawable.ic_lib_jverification,
        31 to R.drawable.ic_lib_kuaishou,
        32 to R.drawable.ic_lib_lua,
        33 to R.drawable.ic_lib_material,
        34 to R.drawable.ic_lib_meizu,
        35 to R.drawable.ic_lib_microsoft,
        36 to R.drawable.ic_lib_netease,
        37 to R.drawable.ic_lib_opencv,
        38 to R.drawable.ic_lib_oppo,
        39 to R.drawable.ic_lib_play_store,
        40 to R.drawable.ic_lib_qiniu,
        41 to R.drawable.ic_lib_react,
        42 to R.drawable.ic_lib_realm,
        43 to R.drawable.ic_lib_rongyun,
        44 to R.drawable.ic_lib_sensors,
        45 to R.drawable.ic_lib_shizuku,
        46 to R.drawable.ic_lib_sql,
        47 to R.drawable.ic_lib_square,
        48 to R.drawable.ic_lib_tencent,
        49 to R.drawable.ic_lib_tencent_ad,
        50 to R.drawable.ic_lib_tencent_cloud,
        51 to R.drawable.ic_lib_tencent_map,
        52 to R.drawable.ic_lib_tensorflow,
        53 to R.drawable.ic_lib_umeng,
        54 to R.drawable.ic_lib_unionpay,
        55 to R.drawable.ic_lib_unity,
        56 to R.drawable.ic_lib_unreal_engine,
        57 to R.drawable.ic_lib_vivo,
        58 to R.drawable.ic_lib_webrtc,
        59 to R.drawable.ic_lib_weibo,
        60 to R.drawable.ic_lib_xamarin,
        61 to R.drawable.ic_lib_xiaoai,
        62 to R.drawable.ic_lib_xiaomi,
        63 to R.drawable.ic_lib_xunfei,
        64 to R.drawable.ic_lib_zhihu,
        65 to R.drawable.ic_kotlin_logo,
        66 to R.drawable.ic_telegram
    )

    fun getResIndex(res: Int): Int {
        MAP.forEach {
            if (it.value == res) {
                return it.key
            }
        }
        return 0
    }

    fun getIconRes(index: Int) = MAP[index] ?: MAP[-1]!!
}