package com.absinthe.libchecker.constant.librarymap

import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.LibChip
import java.util.regex.Pattern

object ProviderLibMap : BaseMap() {
    private val MAP: HashMap<String, LibChip> = hashMapOf(
        Pair(
            "com.huawei.hms.update.provider.UpdateProvider",
            LibChip(R.drawable.ic_lib_huawei, "HMS Update")
        ),
        Pair(
            "com.huawei.updatesdk.fileprovider.UpdateSdkFileProvider",
            LibChip(R.drawable.ic_lib_huawei, "HMS Update")
        ),
        Pair(
            "com.huawei.agconnect.core.provider.AGConnectInitializeProvider",
            LibChip(R.drawable.ic_lib_huawei, "AppGallery Connect")
        ),
        Pair(
            "com.huawei.hms.support.api.push.PushProvider",
            LibChip(R.drawable.ic_lib_huawei, "Huawei Push")
        ),
        Pair(
            "androidx.core.content.FileProvider",
            LibChip(R.drawable.ic_android_outline, "File Provider")
        ),
        Pair(
            "android.support.v4.content.FileProvider",
            LibChip(R.drawable.ic_android_outline, "File Provider")
        ),
        Pair(
            "androidx.lifecycle.ProcessLifecycleOwnerInitializer",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack Lifecycle")
        ),
        Pair(
            "android.arch.lifecycle.ProcessLifecycleOwnerInitializer",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack Lifecycle")
        ),
        Pair(
            "androidx.work.impl.WorkManagerInitializer",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack Work Manager")
        ),
        Pair(
            "androidx.startup.InitializationProvider",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack App Startup")
        ),
        Pair(
            "com.google.firebase.provider.FirebaseInitProvider",
            LibChip(R.drawable.ic_lib_firebase, "Firebase")
        ),
        Pair(
            "com.crashlytics.android.CrashlyticsInitProvider",
            LibChip(R.drawable.ic_lib_firebase, "Crashlytics")
        ),
        Pair(
            "com.google.firebase.perf.provider.FirebasePerfProvider",
            LibChip(R.drawable.ic_lib_firebase, "Firebase Performance")
        ),
        Pair(
            "com.google.android.gms.ads.MobileAdsInitProvider",
            LibChip(R.drawable.ic_lib_google, "Google AdMob")
        ),
        Pair(
            "com.tencent.bugly.beta.utils.BuglyFileProvider",
            LibChip(R.drawable.ic_lib_bugly, "Bugly")
        ),
        Pair(
            "com.tencent.mid.api.MidProvider",
            LibChip(R.drawable.ic_lib_tencent, "腾讯移动分析")
        ),
        Pair(
            "com.facebook.internal.FacebookInitProvider",
            LibChip(R.drawable.ic_lib_facebook, "Facebook SDK")
        ),
        Pair(
            "com.facebook.FacebookContentProvider",
            LibChip(R.drawable.ic_lib_facebook, "Facebook SDK")
        ),
        Pair(
            "com.bytedance.sdk.openadsdk.multipro.TTMultiProvider",
            LibChip(R.drawable.ic_lib_toutiao, "头条广告 SDK")
        ),
        Pair(
            "cn.jpush.android.service.DataProvider",
            LibChip(R.drawable.ic_lib_jpush, "极光推送")
        ),
        Pair(
            "cn.jpush.android.service.Downloadrovider",
            LibChip(R.drawable.ic_lib_jpush, "极光推送")
        ),
        Pair(
            "com.tencent.android.tpush.XGPushProvider",
            LibChip(R.drawable.ic_lib_tencent, "信鸽推送")
        ),
        Pair(
            "com.tencent.android.tpush.SettingsContentProvider",
            LibChip(R.drawable.ic_lib_tencent, "信鸽推送")
        ),
        Pair(
            "com.igexin.download.DownloadProvider",
            LibChip(R.drawable.ic_question, "个推")
        ),
        Pair(
            "com.squareup.picasso.PicassoProvider",
            LibChip(R.drawable.ic_question, "Picasso")
        ),
        Pair(
            "com.blankj.utilcode.util.UtilsFileProvider",
            LibChip(R.drawable.ic_question, "AndroidUtilCode")
        ),
        Pair(
            "com.blankj.utilcode.util.Utils\$FileProvider4UtilCode",
            LibChip(R.drawable.ic_question, "AndroidUtilCode")
        ),
        Pair(
            "com.bytedance.sdk.openadsdk.TTFileProvider",
            LibChip(R.drawable.ic_lib_toutiao, "头条广告 SDK")
        ),
        Pair(
            "com.liulishuo.okdownload.OkDownloadProvider",
            LibChip(R.drawable.ic_question, "OkDownload")
        ),
        Pair(
            "com.netease.nimlib.ipc.NIMContentProvider",
            LibChip(R.drawable.ic_lib_netease, "网易云通信 SDK")
        ),
        Pair(
            "com.sensorsdata.analytics.android.sdk.data.SensorsDataContentProvider",
            LibChip(R.drawable.ic_lib_sensors, "神策分析 SDK")
        ),
        Pair(
            "com.sensorsdata.analytics.android.sdk.SensorsDataContentProvider",
            LibChip(R.drawable.ic_lib_sensors, "神策分析 SDK")
        ),
        Pair(
            "com.umeng.message.provider.MessageProvider",
            LibChip(R.drawable.ic_lib_umeng, "友盟推送")
        ),
        Pair(
            "cn.jpush.android.service.DownloadProvider",
            LibChip(R.drawable.ic_lib_jpush, "极光推送")
        ),
        Pair(
            "cn.bmob.v3.util.BmobContentProvider",
            LibChip(R.drawable.ic_question, "Bmob 后端云")
        ),
        Pair(
            "moe.shizuku.api.ShizukuProvider",
            LibChip(R.drawable.ic_lib_shizuku, "Shizuku")
        ),
        Pair(
            "com.yanzhenjie.permission.FileProvider",
            LibChip(R.drawable.ic_question, "AndPermission")
        ),
        Pair(
            "com.just.agentweb.AgentWebFileProvider",
            LibChip(R.drawable.ic_question, "AgentWeb")
        )
    )

    override fun getMap(): HashMap<String, LibChip> {
        return MAP
    }

    override fun findRegex(name: String): LibChip? {
        return when {
            Pattern.matches("com.qihoo360.replugin.component.process.ProcessPitProvider(.*)", name) -> LibChip(R.drawable.ic_lib_360, "RePlugin", "regex_replugin")
            Pattern.matches("com.qihoo360.replugin.component.provider.PluginPitProvider(.*)", name) -> LibChip(R.drawable.ic_lib_360, "RePlugin", "regex_replugin")
            else -> null
        }
    }
}