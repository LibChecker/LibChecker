package com.absinthe.libchecker.constant

import com.absinthe.libchecker.R

object ServiceLibMap {
    val MAP: HashMap<String, LibChip> = hashMapOf(
        Pair("com.xiaomi.push.service.XMJobService", LibChip(R.drawable.ic_lib_xiaomi, "MiPush")),
        Pair("com.xiaomi.mipush.sdk.PushMessageHandler", LibChip(R.drawable.ic_lib_xiaomi, "MiPush")),
        Pair("com.xiaomi.mipush.sdk.MessageHandleService", LibChip(R.drawable.ic_lib_xiaomi, "MiPush")),
        Pair("com.amap.api.location.APSService", LibChip(R.drawable.ic_lib_amap, "高德地图 SDK")),
        Pair("com.tencent.bugly.beta.tinker.TinkerResultService", LibChip(R.drawable.ic_lib_tencent, "Tinker")),
        Pair("com.tencent.tinker.lib.service.TinkerPatchForeService", LibChip(R.drawable.ic_lib_tencent, "Tinker")),
        Pair("com.tencent.tinker.lib.service.TinkerPatchService", LibChip(R.drawable.ic_lib_tencent, "Tinker")),
        Pair("com.tencent.tinker.lib.service.TinkerPatchService\$InnerService", LibChip(R.drawable.ic_lib_tencent, "Tinker")),
        Pair("com.tencent.tinker.lib.service.DefaultTinkerResultService", LibChip(R.drawable.ic_lib_tencent, "Tinker")),
        Pair("com.google.firebase.messaging.FirebaseMessagingService", LibChip(R.drawable.ic_lib_firebase, "FCM"))
        )
}