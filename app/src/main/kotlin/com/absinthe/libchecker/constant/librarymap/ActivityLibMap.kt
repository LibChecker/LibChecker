package com.absinthe.libchecker.constant.librarymap

import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.LibChip
import java.util.regex.Pattern

object ActivityLibMap : BaseMap() {
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
        Pair(
            "com.qq.e.ads.LandscapeADActivity",
            LibChip(R.drawable.ic_lib_tencent_ad, "腾讯广告 SDK")
        ),
        Pair(
            "com.qq.e.ads.PortraitADActivity",
            LibChip(R.drawable.ic_lib_tencent_ad, "腾讯广告 SDK")
        ),
        Pair(
            "com.qq.e.ads.ADActivity",
            LibChip(R.drawable.ic_lib_tencent_ad, "腾讯广告 SDK")
        ),
        Pair(
            "com.qq.e.ads.RewardvideoPortraitADActivity",
            LibChip(R.drawable.ic_lib_tencent_ad, "腾讯广告 SDK")
        ), Pair(
            "com.qq.e.ads.RewardvideoLandscapeADActivity",
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
        Pair(
            "com.ali.auth.third.ui.LoginWebViewActivity",
            LibChip(R.drawable.ic_lib_alipay, "支付宝 SDK")
        ),
        Pair(
            "com.ali.auth.third.ui.webview.BaseWebViewActivity",
            LibChip(R.drawable.ic_lib_alipay, "支付宝 SDK")
        ),
        Pair(
            "com.ali.auth.third.ui.LoginActivity",
            LibChip(R.drawable.ic_lib_alipay, "支付宝 SDK")
        ),
        Pair(
            "com.alibaba.wireless.security.open.middletier.fc.ui.ContainerActivity",
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
        ),
        Pair(
            "com.bytedance.sdk.openadsdk.activity.TTLandingPageActivity",
            LibChip(R.drawable.ic_lib_toutiao, "头条广告 SDK")
        ),
        Pair(
            "com.bytedance.sdk.openadsdk.activity.TTVideoLandingPageActivity",
            LibChip(R.drawable.ic_lib_toutiao, "头条广告 SDK")
        ),
        Pair(
            "com.bytedance.sdk.openadsdk.activity.TTRewardVideoActivity",
            LibChip(R.drawable.ic_lib_toutiao, "头条广告 SDK")
        ),
        Pair(
            "com.bytedance.sdk.openadsdk.activity.TTFullScreenVideoActivity",
            LibChip(R.drawable.ic_lib_toutiao, "头条广告 SDK")
        ),
        Pair(
            "com.bytedance.sdk.openadsdk.activity.TTDelegateActivity",
            LibChip(R.drawable.ic_lib_toutiao, "头条广告 SDK")
        ),
        Pair(
            "com.bytedance.sdk.openadsdk.activity.TTDelegateActivity",
            LibChip(R.drawable.ic_lib_toutiao, "头条广告 SDK")
        ),
        Pair(
            "com.ss.android.socialbase.appdownloader.view.DownloadSizeLimitActivity",
            LibChip(R.drawable.ic_lib_toutiao, "头条广告 SDK")
        ),
        Pair(
            "com.ss.android.socialbase.appdownloader.view.DownloadTaskDeleteActivity",
            LibChip(R.drawable.ic_lib_toutiao, "头条广告 SDK")
        ),
        Pair(
            "com.ss.android.downloadlib.activity.InteractionMiddleActivity",
            LibChip(R.drawable.ic_lib_toutiao, "头条广告 SDK")
        ),
        Pair(
            "cn.jpush.android.service.JNotifyActivity",
            LibChip(R.drawable.ic_lib_jpush, "极光推送")
        ),
        Pair(
            "cn.jpush.android.ui.PushActivity",
            LibChip(R.drawable.ic_lib_jpush, "极光推送")
        ),
        Pair(
            "cn.jpush.android.ui.PopWinActivity",
            LibChip(R.drawable.ic_lib_jpush, "极光推送")
        ),
        Pair(
            "com.igexin.sdk.PushActivity",
            LibChip(R.drawable.ic_question, "个推")
        ),
        Pair(
            "com.igexin.sdk.GActivity",
            LibChip(R.drawable.ic_question, "个推")
        ),
        Pair(
            "com.xiaomi.account.openauth.AuthorizeActivity",
            LibChip(R.drawable.ic_lib_xiaomi, "小米账号开放平台")
        ),
        Pair(
            "com.blankj.utilcode.util.UtilsTransActivity",
            LibChip(R.drawable.ic_question, "AndroidUtilCode")
        ),
        Pair(
            "com.alibaba.sdk.android.push.keeplive.PushExtActivity",
            LibChip(R.drawable.ic_lib_aliyun, "阿里移动推送")
        )
    )

    override fun getMap(): HashMap<String, LibChip> {
        return MAP
    }

    override fun findRegex(name: String): LibChip? {
        return when {
            Pattern.matches("com.tencent.tinker.loader.hotplug.ActivityStubs(.*)", name) -> LibChip(R.drawable.ic_lib_tencent, "Tinker", "regex_tinker")
            else -> null
        }
    }
}