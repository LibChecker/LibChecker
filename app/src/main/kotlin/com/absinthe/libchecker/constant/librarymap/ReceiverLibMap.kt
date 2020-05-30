package com.absinthe.libchecker.constant.librarymap

import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.LibChip

object ReceiverLibMap {
    val MAP: HashMap<String, LibChip> = hashMapOf(
        Pair("com.xiaomi.push.service.receivers.PingReceiver",
            LibChip(
                R.drawable.ic_lib_xiaomi,
                "MiPush"
            )
        ),
        Pair("com.xiaomi.push.service.receivers.NetworkStatusReceiver",
            LibChip(
                R.drawable.ic_lib_xiaomi,
                "MiPush"
            )
        ),
        Pair("androidx.work.impl.utils.ForceStopRunnable\$BroadcastReceiver",
            LibChip(
                R.drawable.ic_lib_jetpack,
                "Jetpack Work"
            )
        ),
        Pair("androidx.work.impl.background.systemalarm.RescheduleReceiver",
            LibChip(
                R.drawable.ic_lib_jetpack,
                "Jetpack Work"
            )
        ),
        Pair("com.google.firebase.iid.FirebaseInstanceIdReceiver",
            LibChip(
                R.drawable.ic_lib_firebase,
                "Firebase"
            )
        ),
        Pair("com.google.android.gms.measurement.AppMeasurementReceiver",
            LibChip(
                R.drawable.ic_lib_firebase,
                "Firebase Analytics"
            )
        ),
        Pair("com.meizu.cloud.pushsdk.SystemReceiver",
            LibChip(
                R.drawable.ic_lib_meizu,
                "Meizu Push"
            )
        )
    )
}