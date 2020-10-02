package com.absinthe.libchecker.constant.librarymap

import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.LibChip
import java.util.regex.Pattern

object ActivityLibMap : BaseMap() {
    private val MAP: HashMap<String, LibChip> = hashMapOf(
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
            LibChip(R.drawable.ic_lib_google, "Google AdMob")
        ),
        Pair(
            "androidx.slice.compat.SlicePermissionActivity",
            LibChip(R.drawable.ic_lib_jetpack, "Jetpack Slice")
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
            "com.tencent.smtt.sdk.VideoActivity",
            LibChip(R.drawable.ic_lib_tencent, "TBS")
        ),
        Pair(
            "com.tencent.captchasdk.TCaptchaPopupActivity",
            LibChip(R.drawable.ic_lib_tencent, "防水墙")
        ),
        Pair(
            "com.tencent.midas.wx.APMidasWXPayActivity",
            LibChip(R.drawable.ic_lib_tencent, "米大师")
        ),
        Pair(
            "com.tencent.midas.proxyactivity.APMidasPayProxyActivity",
            LibChip(R.drawable.ic_lib_tencent, "米大师")
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
        ), 
        Pair(
            "com.qq.e.ads.RewardvideoLandscapeADActivity",
            LibChip(R.drawable.ic_lib_tencent_ad, "腾讯广告 SDK")
        ),
        Pair(
            "com.tencent.bugly.beta.ui.BetaActivity",
            LibChip(R.drawable.ic_lib_bugly, "Bugly")
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
            "com.huawei.hms.activity.BridgeActivity",
            LibChip(R.drawable.ic_lib_huawei, "HMS Core")
        ),
        Pair(
            "com.huawei.android.hms.agent.common.HMSAgentActivity",
            LibChip(R.drawable.ic_lib_huawei, "HMS Core")
        ),
        Pair(
            "com.huawei.android.hms.agent.pay.HMSPayAgentActivity",
            LibChip(R.drawable.ic_lib_huawei, "HMS Core")
        ),
        Pair(
            "com.huawei.android.hms.agent.hwid.HMSSignInAgentActivity",
            LibChip(R.drawable.ic_lib_huawei, "HMS Core")
        ),
        Pair(
            "com.huawei.hms.dtm.PreviewActivity",
            LibChip(R.drawable.ic_lib_huawei, "Huawei DTM")
        ),
        Pair(
            "com.huawei.openalliance.ad.activity.PPSLauncherActivity",
            LibChip(R.drawable.ic_lib_huawei, "Huawei Ads SDK")
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
            LibChip(R.drawable.ic_sdk_placeholder, "uCrop")
        ),
        Pair(
            "com.yalantis.ucrop.PictureMultiCuttingActivity",
            LibChip(R.drawable.ic_sdk_placeholder, "uCrop")
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
            LibChip(R.drawable.ic_sdk_placeholder, "个推")
        ),
        Pair(
            "com.igexin.sdk.GActivity",
            LibChip(R.drawable.ic_sdk_placeholder, "个推")
        ),
        Pair(
            "com.xiaomi.account.openauth.AuthorizeActivity",
            LibChip(R.drawable.ic_lib_xiaomi, "小米账号开放平台")
        ),
        Pair(
            "com.blankj.utilcode.util.UtilsTransActivity",
            LibChip(R.drawable.ic_sdk_placeholder, "AndroidUtilCode")
        ),
        Pair(
            "com.blankj.utilcode.util.UtilsTransActivity4MainProcess",
            LibChip(R.drawable.ic_sdk_placeholder, "AndroidUtilCode")
        ),
        Pair(
            "com.alibaba.sdk.android.push.keeplive.PushExtActivity",
            LibChip(R.drawable.ic_lib_aliyun, "阿里移动推送")
        ),
        Pair(
            "com.taobao.weex.WXGlobalEventReceiver",
            LibChip(R.drawable.ic_lib_alibaba, "Weex")
        ),
        Pair(
            "cmb.pb.ui.PBKeyboardActivity",
            LibChip(R.drawable.ic_lib_cmb, "招商银行 SDK")
        ),
        Pair(
            "org.hapjs.features.channel.transparentactivity.TransparentActivity",
            LibChip(R.drawable.ic_lib_hapjs, "快应用")
        ),
        Pair(
            "com.unionpay.uppay.PayActivity",
            LibChip(R.drawable.ic_lib_unionpay, "银联 SDK")
        ),
        Pair(
            "com.unionpay.UPPayWapActivity",
            LibChip(R.drawable.ic_lib_unionpay, "银联 SDK")
        ),
        Pair(
            "net.openid.appauth.RedirectUriReceiverActivity",
            LibChip(R.drawable.ic_lib_appauth, "AppAuth")
        ),
        Pair(
            "net.openid.appauth.AuthorizationManagementActivity",
            LibChip(R.drawable.ic_lib_appauth, "AppAuth")
        ),
        Pair(
            "com.pingplusplus.android.PaymentActivity",
            LibChip(R.drawable.ic_sdk_placeholder, "Ping++")
        ),
        Pair(
            "com.unity3d.player.UnityPlayerActivity",
            LibChip(R.drawable.ic_lib_unity, "Unity")
        ),
        Pair(
            "com.soundcloud.android.crop.CropImageActivity",
            LibChip(R.drawable.ic_sdk_placeholder, "android-crop")
        ),
        Pair(
            "com.yanzhenjie.permission.bridge.BridgeActivity",
            LibChip(R.drawable.ic_sdk_placeholder, "AndPermission")
        ),
        Pair(
            "com.yanzhenjie.permission.PermissionActivity",
            LibChip(R.drawable.ic_sdk_placeholder, "AndPermission")
        ),
        Pair(
            "com.google.ar.core.InstallActivity",
            LibChip(R.drawable.ic_lib_google_arcore, "ARCore")
        ),
        Pair(
            "com.cmic.sso.sdk.activity.LoginAuthActivity",
            LibChip(R.drawable.ic_lib_jverification, "极光认证")
        ),
        Pair(
            "pub.devrel.easypermissions.AppSettingsDialogHolderActivity",
            LibChip(R.drawable.ic_sdk_placeholder, "EasyPermissions")
        ),
        Pair(
            "com.bestpay.app.H5PayActivity",
            LibChip(R.drawable.ic_sdk_placeholder, "翼支付")
        ),
        Pair(
            "com.baidu.android.pushservice.hwproxy.HwNotifyActivity",
            LibChip(R.drawable.ic_lib_baidu, "百度云推送")
        ),
        Pair(
            "com.baidu.mobads.AppActivity",
            LibChip(R.drawable.ic_lib_baidu, "移动应用推广 SDK")
        ),
        Pair(
            "com.kuaishou.commercial.downloader.center.AdDownloadCenterV2Activity",
            LibChip(R.drawable.ic_lib_kuaishou, "快手广告 SDK")
        ),
        Pair(
            "com.tencent.android.tpush.TpnsActivity",
            LibChip(R.drawable.ic_lib_tencent_cloud, "腾讯移动推送")
        ),
        Pair(
            "com.umeng.socialize.media.WBShareCallBackActivity",
            LibChip(R.drawable.ic_lib_umeng, "U-Share")
        ),
    )

    override fun getMap(): HashMap<String, LibChip> {
        return MAP
    }

    private val PATTERN_TINKER = Pattern.compile("com.tencent.tinker.loader.hotplug.ActivityStubs(.*)")
    private val PATTERN_PANGLE_1 = Pattern.compile("com.bytedance.sdk.openadsdk.activity.(.*)")
    private val PATTERN_PANGLE_2 = Pattern.compile("com.ss.android.socialbase.appdownloader.(.*)")
    private val PATTERN_PANGLE_3 = Pattern.compile("com.ss.android.downloadlib.(.*)")
    private val PATTERN_KSAD_1 = Pattern.compile("com.yxcorp.gifshow.ad(.*)")
    private val PATTERN_KSAD_2 = Pattern.compile("com.yxcorp.map.advertisement.(.*)")

    override fun findRegex(name: String): LibChip? {
        return when {
            PATTERN_TINKER.matcher(name).matches() -> LibChip(R.drawable.ic_lib_tencent, "Tinker", "regex_tinker")
            matchAllPatterns(name, PATTERN_PANGLE_1, PATTERN_PANGLE_2, PATTERN_PANGLE_3) -> LibChip(R.drawable.ic_lib_bytedance, "Pangle SDK", "regex_pangle")
            matchAllPatterns(name, PATTERN_KSAD_1, PATTERN_KSAD_2) -> LibChip(R.drawable.ic_lib_kuaishou, "快手广告 SDK", "regex_kuaishou_ad")
            else -> null
        }
    }
}