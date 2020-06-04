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
            "com.android.billingclient.api.ProxyBillingActivity",
            LibChip(R.drawable.ic_lib_play_store, "Google Play Billing")
        ),
        Pair(
            "com.google.android.gms.auth.api.signin.internal.SignInHubActivity",
            LibChip(R.drawable.ic_lib_google, "Google Sign-In")
        ),
        Pair(
            "com.google.android.gms.ads.AdActivity",
            LibChip(R.drawable.ic_lib_google, "Google Mobile Ads")
        ),
        Pair(
            "com.tencent.tauth.AuthActivity",
            LibChip(R.drawable.ic_lib_tencent, "腾讯开放平台")
        ),
        Pair(
            "com.tencent.connect.common.AssistActivity",
            LibChip(R.drawable.ic_lib_tencent, "腾讯开放平台")
        ),
        Pair("com.qq.e.ads.LandscapeADActivity",
            LibChip(R.drawable.ic_lib_tencent_ad, "腾讯广告 SDK")
        ),
        Pair("com.qq.e.ads.PortraitADActivity",
            LibChip(R.drawable.ic_lib_tencent_ad, "腾讯广告 SDK")
        ),
        Pair("com.qq.e.ads.ADActivity",
            LibChip(R.drawable.ic_lib_tencent_ad, "腾讯广告 SDK")
        ),
        Pair(
            "com.tencent.bugly.beta.ui.BetaActivity",
            LibChip(R.drawable.ic_lib_bugly, "Bugly")
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
            "com.alipay.sdk.auth.AuthActivity",
            LibChip(R.drawable.ic_lib_alipay, "支付宝 SDK")
        ),
        Pair(
            "com.alipay.sdk.app.H5OpenAuthActivity",
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
        Pair("com.alibaba.wireless.security.open.middletier.fc.ui.ContainerActivity",
            LibChip(R.drawable.ic_lib_ali_security, "阿里聚安全")
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
            "com.sina.weibo.sdk.component.WeiboSdkBrowser",
            LibChip(R.drawable.ic_lib_weibo, "微博 SDK")
        ),
        Pair(
            "com.vivo.push.sdk.LinkProxyClientActivity",
            LibChip(R.drawable.ic_lib_vivo, "vivo Push")
        ),
        Pair(
            "com.facebook.react.devsupport.DevSettingsActivity",
            LibChip(R.drawable.ic_lib_react, "React Native")
        ),
        Pair(
            "com.facebook.FacebookActivity",
            LibChip(R.drawable.ic_lib_facebook, "Facebook SDK")
        ),
        Pair(
            "com.facebook.CustomTabActivity",
            LibChip(R.drawable.ic_lib_facebook, "Facebook SDK")
        ),
        Pair(
            "com.facebook.CustomTabMainActivity",
            LibChip(R.drawable.ic_lib_facebook, "Facebook SDK")
        ),
        Pair(
            "com.yalantis.ucrop.UCropActivity",
            LibChip(R.drawable.ic_question, "uCrop")
        ),
        Pair(
            "com.zhihu.matisse.internal.ui.AlbumPreviewActivity",
            LibChip(R.drawable.ic_lib_zhihu, "Matisse")
        ),
        Pair(
            "com.zhihu.matisse.internal.ui.SelectedPreviewActivity",
            LibChip(R.drawable.ic_lib_zhihu, "Matisse")
        ),
        Pair(
            "com.zhihu.matisse.ui.MatisseActivity",
            LibChip(R.drawable.ic_lib_zhihu, "Matisse")
        )
    )
}