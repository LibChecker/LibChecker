package com.absinthe.libchecker.constant.librarymap

import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.LibChip

object ActivityLibMap {
    val MAP: HashMap<String, LibChip> = hashMapOf(
        Pair(
            "com.google.android.gms.common.api.GoogleApiActivity",
            LibChip(R.drawable.ic_lib_play_store, "Google Play Service")
        ),
        Pair(
            "com.google.android.gms.auth.api.signin.internal.SignInHubActivity",
            LibChip(R.drawable.ic_lib_google, "Google Sign-In")
        ),
        Pair(
            "com.tencent.tauth.AuthActivity",
            LibChip(R.drawable.ic_lib_tencent, "腾讯开放平台")
        ),
        Pair(
            "com.tencent.connect.common.AssistActivity",
            LibChip(R.drawable.ic_lib_tencent, "腾讯开放平台")
        ),
        Pair(
            "com.huawei.hms.activity.BridgeActivity",
            LibChip(R.drawable.ic_lib_huawei, "HMS SDK")
        ),
        Pair(
            "com.huawei.updatesdk.service.otaupdate.AppUpdateActivity",
            LibChip(R.drawable.ic_lib_huawei, "HMS Update")
        ),
        Pair(
            "com.huawei.updatesdk.support.pm.PackageInstallerActivity",
            LibChip(R.drawable.ic_lib_huawei, "HMS Update")
        ),
        Pair(
            "com.huawei.android.hms.agent.common.HMSAgentActivity",
            LibChip(R.drawable.ic_lib_huawei, "HMS SDK")
        ),
        Pair(
            "com.huawei.android.hms.agent.pay.HMSPayAgentActivity",
            LibChip(R.drawable.ic_lib_huawei, "HMS SDK")
        ),
        Pair(
            "com.huawei.android.hms.agent.hwid.HMSSignInAgentActivity",
            LibChip(R.drawable.ic_lib_huawei, "HMS SDK")
        ),
        Pair(
            "com.alipay.sdk.app.H5PayActivity",
            LibChip(R.drawable.ic_lib_alipay, "支付宝 SDK")
        ),
        Pair(
            "com.alipay.sdk.app.H5AuthActivity",
            LibChip(R.drawable.ic_lib_alipay, "支付宝 SDK")
        ),
        Pair(
            "com.alipay.sdk.app.PayResultActivity",
            LibChip(R.drawable.ic_lib_alipay, "支付宝 SDK")
        ),
        Pair(
            "com.alipay.sdk.app.AlipayResultActivity",
            LibChip(R.drawable.ic_lib_alipay, "支付宝 SDK")
        ),
        Pair(
            "com.sina.weibo.sdk.share.WbShareTransActivity",
            LibChip(R.drawable.ic_lib_weibo, "微博 SDK")
        ),
        Pair(
            "com.sina.weibo.sdk.share.WbShareToStoryActivity",
            LibChip(R.drawable.ic_lib_weibo, "微博 SDK")
        ),
        Pair(
            "com.sina.weibo.sdk.share.WbShareResultActivity",
            LibChip(R.drawable.ic_lib_weibo, "微博 SDK")
        ),
        Pair(
            "com.sina.weibo.sdk.web.WeiboSdkWebActivity",
            LibChip(R.drawable.ic_lib_weibo, "微博 SDK")
        ),
        Pair(
            "com.vivo.push.sdk.LinkProxyClientActivity",
            LibChip(R.drawable.ic_lib_vivo, "vivo Push")
        )
    )
}