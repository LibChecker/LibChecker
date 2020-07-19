package com.absinthe.libchecker.constant.librarymap

import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.LibChip

object ReceiverLibMap : BaseMap() {
    private val MAP: HashMap<String, LibChip> = hashMapOf(
        Pair(
            "com.xiaomi.push.service.receivers.PingReceiver",
            LibChip(
                R.drawable.ic_lib_xiaomi,
                "MiPush"
            )
        ),
        Pair(
            "com.xiaomi.push.service.receivers.NetworkStatusReceiver",
            LibChip(
                R.drawable.ic_lib_xiaomi,
                "MiPush"
            )
        ),
        Pair(
            "androidx.work.impl.utils.ForceStopRunnable\$BroadcastReceiver",
            LibChip(
                R.drawable.ic_lib_jetpack,
                "Jetpack Work Manager"
            )
        ),
        Pair(
            "androidx.work.impl.background.systemalarm.RescheduleReceiver",
            LibChip(
                R.drawable.ic_lib_jetpack,
                "Jetpack Work Manager"
            )
        ),
        Pair(
            "androidx.work.impl.diagnostics.DiagnosticsReceiver",
            LibChip(
                R.drawable.ic_lib_jetpack,
                "Jetpack Work Manager"
            )
        ),
        Pair(
            "androidx.media.session.MediaButtonReceiver",
            LibChip(
                R.drawable.ic_lib_jetpack,
                "Jetpack Media"
            )
        ),
        Pair(
            "androidx.remotecallback.ProviderRelayReceiver",
            LibChip(
                R.drawable.ic_lib_jetpack,
                "Jetpack Remote Callback"
            )
        ),
        Pair(
            "com.google.firebase.iid.FirebaseInstanceIdReceiver",
            LibChip(
                R.drawable.ic_lib_firebase,
                "Firebase"
            )
        ),
        Pair(
            "com.google.android.datatransport.runtime.scheduling.jobscheduling.AlarmManagerSchedulerBroadcastReceiver",
            LibChip(
                R.drawable.ic_lib_firebase,
                "Firebase"
            )
        ),
        Pair(
            "com.google.android.gms.measurement.AppMeasurementReceiver",
            LibChip(
                R.drawable.ic_lib_firebase,
                "Firebase Analytics"
            )
        ),
        Pair(
            "com.google.android.gms.measurement.AppMeasurementInstallReferrerReceiver",
            LibChip(
                R.drawable.ic_lib_firebase,
                "Firebase Analytics"
            )
        ),
        Pair(
            "com.google.android.gms.analytics.AnalyticsReceiver",
            LibChip(
                R.drawable.ic_lib_google_analytics,
                "Google Analytics"
            )
        ),
        Pair(
            "com.google.android.gms.cast.framework.media.MediaIntentReceiver",
            LibChip(
                R.drawable.ic_lib_google,
                "Google Cast"
            )
        ),
        Pair(
            "com.meizu.cloud.pushsdk.SystemReceiver",
            LibChip(
                R.drawable.ic_lib_meizu,
                "Meizu Push"
            )
        ),
        Pair(
            "com.huawei.hms.support.api.push.PushEventReceiver",
            LibChip(
                R.drawable.ic_lib_huawei,
                "Huawei Push"
            )
        ),
        Pair(
            "com.huawei.hms.support.api.push.PushReceiver",
            LibChip(
                R.drawable.ic_lib_huawei,
                "Huawei Push"
            )
        ),
        Pair(
            "com.huawei.hms.support.api.push.PushMsgReceiver",
            LibChip(
                R.drawable.ic_lib_huawei,
                "Huawei Push"
            )
        ),
        Pair(
            "com.huawei.android.pushagent.PushEventReceiver",
            LibChip(
                R.drawable.ic_lib_huawei,
                "Huawei Push"
            )
        ),
        Pair(
            "com.huawei.android.pushagent.PushBootReceiver",
            LibChip(
                R.drawable.ic_lib_huawei,
                "Huawei Push"
            )
        ),
        Pair(
            "com.facebook.CurrentAccessTokenExpirationBroadcastReceiver",
            LibChip(
                R.drawable.ic_lib_facebook,
                "Facebook SDK"
            )
        ),
        Pair(
            "com.taobao.accs.EventReceiver",
            LibChip(
                R.drawable.ic_lib_aliyun,
                "阿里移动推送"
            )
        ),
        Pair(
            "com.taobao.accs.ServiceReceiver",
            LibChip(
                R.drawable.ic_lib_aliyun,
                "阿里移动推送"
            )
        ),
        Pair(
            "com.taobao.agoo.AgooCommondReceiver",
            LibChip(
                R.drawable.ic_lib_aliyun,
                "阿里移动推送"
            )
        ),
        Pair(
            "com.alibaba.sdk.android.push.SystemEventReceiver",
            LibChip(
                R.drawable.ic_lib_aliyun,
                "阿里移动推送"
            )
        ),
        Pair(
            "org.android.agoo.xiaomi.MiPushBroadcastReceiver",
            LibChip(
                R.drawable.ic_lib_aliyun,
                "MiPush(Aliyun Proxy)"
            )
        ),
        Pair(
            "org.android.agoo.huawei.HuaweiPushReceiver",
            LibChip(
                R.drawable.ic_lib_aliyun,
                "Huawei Push(Aliyun Proxy)"
            )
        ),
        Pair(
            "org.android.agoo.vivo.PushMessageReceiverImpl",
            LibChip(
                R.drawable.ic_lib_aliyun,
                "vivo Push(Aliyun Proxy)"
            )
        ),
        Pair(
            "com.ss.android.downloadlib.core.download.DownloadReceiver",
            LibChip(
                R.drawable.ic_lib_toutiao,
                "头条广告 SDK"
            )
        ),
        Pair(
            "com.igexin.sdk.PushReceiver",
            LibChip(
                R.drawable.ic_question,
                "个推"
            )
        ),
        Pair(
            "cn.jpush.android.service.AlarmReceiver",
            LibChip(
                R.drawable.ic_lib_jpush,
                "极光推送"
            )
        ),
        Pair(
            "cn.jpush.android.service.PushReceiver",
            LibChip(
                R.drawable.ic_lib_jpush,
                "极光推送"
            )
        ),
        Pair(
            "cn.jpush.android.service.PluginXiaomiPlatformsReceiver",
            LibChip(
                R.drawable.ic_lib_jpush,
                "极光推送"
            )
        ),
        Pair(
            "com.tencent.android.tpush.XGPushReceiver",
            LibChip(
                R.drawable.ic_lib_tencent,
                "信鸽推送"
            )
        ),
        Pair(
            "com.umeng.message.NotificationProxyBroadcastReceiver",
            LibChip(
                R.drawable.ic_lib_umeng,
                "友盟推送"
            )
        ),
        Pair(
            "com.netease.nimlib.service.NimReceiver",
            LibChip(
                R.drawable.ic_lib_netease,
                "网易云通信 SDK"
            )
        ),
        Pair(
            "com.netease.nimlib.service.ResponseReceiver",
            LibChip(
                R.drawable.ic_lib_netease,
                "网易云通信 SDK"
            )
        ),
        Pair(
            "com.microsoft.appcenter.distribute.DownloadManagerReceiver",
            LibChip(
                R.drawable.ic_lib_microsoft,
                "App Center"
            )
        ),
        Pair(
            "com.taobao.weex.WXGlobalEventReceiver",
            LibChip(
                R.drawable.ic_lib_alibaba,
                "Weex"
            )
        ),
        Pair(
            "com.evernote.android.job.JobBootReceiver",
            LibChip(
                R.drawable.ic_lib_evernote,
                "Android-Job"
            )
        ),
        Pair(
            "com.evernote.android.job.v14.PlatformAlarmReceiver",
            LibChip(
                R.drawable.ic_lib_evernote,
                "Android-Job"
            )
        )
    )

    override fun getMap(): HashMap<String, LibChip> {
        return MAP
    }

    override fun findRegex(name: String): LibChip? {
        return null
    }
}