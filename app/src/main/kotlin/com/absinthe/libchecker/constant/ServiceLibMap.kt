package com.absinthe.libchecker.constant

import com.absinthe.libchecker.R

object ServiceLibMap {
    val MAP: HashMap<String, LibChip> = hashMapOf(
        Pair("com.xiaomi.push.service.XMJobService", LibChip(R.drawable.ic_lib_xiaomi, "MiPush")),
        Pair("com.xiaomi.mipush.sdk.PushMessageHandler", LibChip(R.drawable.ic_lib_xiaomi, "MiPush")),
        Pair("com.xiaomi.mipush.sdk.MessageHandleService", LibChip(R.drawable.ic_lib_xiaomi, "MiPush"))
        )
}