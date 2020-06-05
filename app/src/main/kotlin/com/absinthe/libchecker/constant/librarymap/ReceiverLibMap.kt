package com.absinthe.libchecker.constant.librarymap

import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.LibChip

object ReceiverLibMap : BaseMap() {
    val MAP: HashMap<String, LibChip> = hashMapOf(
        Pair(
            "com.xiaomi.push.service.receivers.PingReceiver",
            LibChip(
                R.drawable.ic_lib_xiaomi,
                "MiPush"
            )
        ),
        Pair(
            "org.android.agoo.xiaomi.MiPushBroadcastReceiver",
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
                "Jetpack Work"
            )
        ),
        Pair(
            "androidx.work.impl.background.systemalarm.RescheduleReceiver",
            LibChip(
                R.drawable.ic_lib_jetpack,
                "Jetpack Work"
            )
        ),
        Pair(
            "androidx.work.impl.diagnostics.DiagnosticsReceiver",
            LibChip(
                R.drawable.ic_lib_jetpack,
                "Jetpack Work"
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
            "com.facebook.CurrentAccessTokenExpirationBroadcastReceiver",
            LibChip(
                R.drawable.ic_lib_facebook,
                "Facebook SDK"
            )
        ),
        Pair(
            "com.taobao.accs.EventReceiver",
            LibChip(
                R.drawable.ic_lib_taobao,
                "淘宝推送"
            )
        ),
        Pair(
            "com.taobao.agoo.AgooCommondReceiver",
            LibChip(
                R.drawable.ic_lib_taobao,
                "淘宝推送"
            )
        ),
        Pair(
            "com.ss.android.downloadlib.core.download.DownloadReceiver",
            LibChip(
                R.drawable.ic_lib_toutiao,
                "头条广告 SDK"
            )
        )
    )

    override fun getMap(): HashMap<String, LibChip> {
        return MAP
    }
}