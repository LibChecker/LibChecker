package com.absinthe.libchecker.constant.librarymap

import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.LibChip

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
            "com.xiaomi.mipush",
            LibChip(R.drawable.ic_lib_xiaomi, "MiPush")
        ),
        Pair(
            "kotlin.reflect",
            LibChip(R.drawable.ic_kotlin_logo, "Kotlin Reflect")
        ),
        Pair(
            "kotlin.coroutines.jvm",
            LibChip(R.drawable.ic_kotlin_logo, "Kotlin Coroutines")
        ),
        Pair(
            "kotlin.coroutines",
            LibChip(R.drawable.ic_kotlin_logo, "Kotlin Coroutines")
        ),
        Pair(
            "kotlinx.coroutines",
            LibChip(R.drawable.ic_kotlin_logo, "Kotlin Coroutines")
        ),
        Pair(
            "kotlin.sequences",
            LibChip(R.drawable.ic_kotlin_logo, "Kotlin Sequences")
        ),
        Pair(
            "kotlin.annotation",
            LibChip(R.drawable.ic_kotlin_logo, "Kotlin")
        ),
        Pair(
            "kotlin.internal",
            LibChip(R.drawable.ic_kotlin_logo, "Kotlin")
        ),
        Pair(
            "kotlin.contracts",
            LibChip(R.drawable.ic_kotlin_logo, "Kotlin")
        ),
        Pair(
            "kotlin.random",
            LibChip(R.drawable.ic_kotlin_logo, "Kotlin")
        ),
        Pair(
            "kotlin.ranges",
            LibChip(R.drawable.ic_kotlin_logo, "Kotlin")
        ),
        Pair(
            "kotlin.comparisions",
            LibChip(R.drawable.ic_kotlin_logo, "Kotlin")
        ),
        Pair(
            "okhttp3.internal",
            LibChip(R.drawable.ic_sdk_placeholder, "OKHttp3")
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
    )

    override fun getMap(): HashMap<String, LibChip> {
        return MAP
    }

    override fun findRegex(name: String): LibChip? {
        return null
    }
}