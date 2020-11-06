package com.absinthe.libchecker.constant.librarymap

import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.LibChip
import java.util.regex.Pattern

/**
 * <pre>
 * author : Absinthe
 * time : 2020/08/08
 * </pre>
 */
object DexLibMap : BaseMap() {

    private val MAP: HashMap<String, LibChip> = hashMapOf(
        Pair(
            "android.support.v4",
            LibChip(R.drawable.ic_lib_android, "Support Library")
        ),
        Pair(
            "android.support.v7",
            LibChip(R.drawable.ic_lib_android, "Support Library")
        ),
        Pair(
            "androidx.versionedparcelable",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack Versionedparcelable")
        ),
        Pair(
            "androidx.annotation",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack Annotation")
        ),
        Pair(
            "androidx.core",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack Core")
        ),
        Pair(
            "androidx.lifecycle",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack Lifecycle")
        ),
        Pair(
            "androidx.activity",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack Activity")
        ),
        Pair(
            "androidx.savedstate",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack Savedstate")
        ),
        Pair(
            "androidx.coordinatorlayout",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack CoordinatorLayout")
        ),
        Pair(
            "androidx.recyclerview",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack RecyclerView")
        ),
        Pair(
            "androidx.appcompat",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack AppCompat")
        ),
        Pair(
            "androidx.fragment",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack Fragment")
        ),
        Pair(
            "androidx.viewpager",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack ViewPager")
        ),
        Pair(
            "androidx.cardview",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack CardView")
        ),
        Pair(
            "androidx.drawerlayout",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack DrawerLayout")
        ),
        Pair(
            "androidx.media",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack Media")
        ),
        Pair(
            "androidx.swiperefreshlayout",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack SwipeRefreshLayout")
        ),
        Pair(
            "androidx.constraintlayout",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack ConstraintLayout")
        ),
        Pair(
            "androidx.customview",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack CustomView")
        ),
        Pair(
            "androidx.viewpager2",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack ViewPager2")
        ),
        Pair(
            "androidx.loader",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack Loader")
        ),
        Pair(
            "androidx.legacy",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack Legacy")
        ),
        Pair(
            "androidx.transition",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack Transition")
        ),
        Pair(
            "androidx.vectordrawable",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack VectorDrawable")
        ),
        Pair(
            "androidx.slidingpanelayout",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack SlidingPaneLayout")
        ),
        Pair(
            "androidx.preference",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack Preference")
        ),
        Pair(
            "androidx.browser",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack Browser")
        ),
        Pair(
            "androidx.room",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack Room")
        ),
        Pair(
            "androidx.collection",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack Collection")
        ),
        Pair(
            "androidx.arch",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack Arch Core")
        ),
        Pair(
            "androidx.interpolator",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack Interpolator")
        ),
        Pair(
            "androidx.cursoradapter",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack CursorAdapter")
        ),
        Pair(
            "androidx.asynclayoutinflater",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack AsyncLayoutInflater")
        ),
        Pair(
            "androidx.localbroadcastmanager",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack LocalBroadcastManager")
        ),
        Pair(
            "androidx.multidex",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack MultiDex")
        ),
        Pair(
            "androidx.print",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack Print")
        ),
        Pair(
            "androidx.documentfile",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack DocumentFile")
        ),
        Pair(
            "androidx.work",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack Work Manager")
        ),
        Pair(
            "androidx.databinding",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack DataBinding")
        ),
        Pair(
            "androidx.exifinterface",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack ExifInterface")
        ),
        Pair(
            "androidx.sqlite",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack SQLite")
        ),
        Pair(
            "com.xiaomi.mipush",
            LibChip(R.drawable.ic_lib_xiaomi, "MiPush")
        ),
        Pair(
            "com.xiaomi.push",
            LibChip(R.drawable.ic_lib_xiaomi, "MiPush")
        ),
        Pair(
            "com.xiaomi.xmpush",
            LibChip(R.drawable.ic_lib_xiaomi, "MiPush")
        ),
        Pair(
            "kotlin.reflect",
            LibChip(R.drawable.ic_kotlin_logo, "Kotlin Reflect")
        ),
        Pair(
            "kotlin.sequences",
            LibChip(R.drawable.ic_kotlin_logo, "Kotlin Sequences")
        ),
        Pair(
            "com.google.android.material",
            LibChip(R.drawable.ic_lib_material, "Material Design Components")
        ),
        Pair(
            "com.google.android.gms",
            LibChip(R.drawable.ic_lib_play_store, "Google Mobile Service")
        ),
        Pair(
            "com.google.android.exoplayer2",
            LibChip(R.drawable.ic_lib_google, "ExoPlayer")
        ),
        Pair(
            "com.google.android.flexbox",
            LibChip(R.drawable.ic_lib_google, "FlexBox")
        ),
        Pair(
            "com.google.gson",
            LibChip(R.drawable.ic_lib_google, "gson")
        ),
        Pair(
            "com.google.protobuf",
            LibChip(R.drawable.ic_lib_google, "Protocol Buffer")
        ),
        Pair(
            "com.google.zxing",
            LibChip(R.drawable.ic_lib_google, "ZXing")
        ),
        Pair(
            "com.google.firebase",
            LibChip(R.drawable.ic_lib_firebase, "Firebase")
        ),
        Pair(
            "com.bumptech.glide",
            LibChip(R.drawable.ic_sdk_placeholder, "Glide")
        ),
        Pair(
            "com.airbnb.lottie",
            LibChip(R.drawable.ic_lib_airbnb, "Lottie")
        ),
        Pair(
            "com.sina.weibo",
            LibChip(R.drawable.ic_lib_weibo, "微博 SDK")
        ),
        Pair(
            "org.greenbot.eventbus",
            LibChip(R.drawable.ic_sdk_placeholder, "EventBus")
        ),
        Pair(
            "org.greenbot.greendao",
            LibChip(R.drawable.ic_sdk_placeholder, "GreenDAO")
        ),
        Pair(
            "com.tencent.bugly",
            LibChip(R.drawable.ic_lib_bugly, "Bugly")
        ),
        Pair(
            "com.tencent.smtt",
            LibChip(R.drawable.ic_lib_tencent, "TBS")
        ),
        Pair(
            "com.tencent.tbs",
            LibChip(R.drawable.ic_lib_tencent, "TBS")
        ),
        Pair(
            "com.tencent.map",
            LibChip(R.drawable.ic_lib_tencent_map, "腾讯地图 SDK")
        ),
        Pair(
            "com.alibaba.fastjson",
            LibChip(R.drawable.ic_lib_alibaba, "fastjson")
        ),
        Pair(
            "butterknife.internal",
            LibChip(R.drawable.ic_sdk_placeholder, "Butter Knife")
        ),
        Pair(
            "butterknife.runtime",
            LibChip(R.drawable.ic_sdk_placeholder, "Butter Knife")
        ),
        Pair(
            "com.microsoft.appcenter",
            LibChip(R.drawable.ic_lib_microsoft, "App Center")
        ),
        Pair(
            "com.facebook.fresco",
            LibChip(R.drawable.ic_lib_facebook, "Fresco")
        ),
        Pair(
            "com.ta.utdid2",
            LibChip(R.drawable.ic_lib_aliyun, "UTDID")
        ),
        Pair(
            "com.amap.api",
            LibChip(R.drawable.ic_lib_amap, "高德 SDK")
        ),
        Pair(
            "com.huawei.hianalytics",
            LibChip(R.drawable.ic_lib_huawei, "HMS Analytics Kit")
        ),
        Pair(
            "tv.danmaku.ijk",
            LibChip(R.drawable.ic_lib_bilibili, "IJKPlayer")
        ),
    )

    override fun getMap(): HashMap<String, LibChip> {
        return MAP
    }

    private val PATTERN_KOTLIN_COROUTINES_1 = Pattern.compile("kotlin.coroutines.(.*)")
    private val PATTERN_KOTLIN_COROUTINES_2 = Pattern.compile("kotlinx.coroutines.(.*)")
    private val PATTERN_KOTLIN = Pattern.compile("kotlin.(.*)")
    private val PATTERN_OKHTTP3 = Pattern.compile("okhttp3.(.*)")
    private val PATTERN_RETROFIT2 = Pattern.compile("retrofit2.(.*)")
    private val PATTERN_OKIO = Pattern.compile("okio.(.*)")
    private val PATTERN_ALIPAY = Pattern.compile("com.alipay.(.*)")
    private val PATTERN_FLUTTER = Pattern.compile("io.flutter.(.*)")

    override fun findRegex(name: String): LibChip? {
        return when {
            PATTERN_KOTLIN.matcher(name).matches() -> LibChip(R.drawable.ic_kotlin_logo, "Kotlin", "regex_kotlin")
            matchAllPatterns(name, PATTERN_KOTLIN_COROUTINES_1, PATTERN_KOTLIN_COROUTINES_2) ->
                LibChip(R.drawable.ic_kotlin_logo, "Kotlin Coroutines", "regex_kotlin_coroutines")
            PATTERN_OKHTTP3.matcher(name).matches() -> LibChip(R.drawable.ic_lib_square, "OKHttp3", "regex_okhttp3")
            PATTERN_RETROFIT2.matcher(name).matches() -> LibChip(R.drawable.ic_lib_square, "Retrofit2", "regex_retrofit2")
            PATTERN_OKIO.matcher(name).matches() -> LibChip(R.drawable.ic_lib_square, "Okio", "regex_okio")
            PATTERN_ALIPAY.matcher(name).matches() -> LibChip(R.drawable.ic_lib_alipay, "支付宝 SDK", "regex_alipay")
            PATTERN_FLUTTER.matcher(name).matches() -> LibChip(R.drawable.ic_lib_flutter, "Flutter", "regex_flutter")
            else -> null
        }
    }

    val DEEP_LEVEL_3_SET = hashSetOf(
        "com.google.android",
        "com.samsung.android",
        "com.alibaba.android",
        "cn.com.chinatelecom",
        "com.github.chrisbanes",
        "com.google.thirdparty",
        ""
    )
}