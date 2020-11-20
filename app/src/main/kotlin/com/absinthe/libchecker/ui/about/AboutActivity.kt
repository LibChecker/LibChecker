package com.absinthe.libchecker.ui.about

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.PersistableBundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.extensions.addPaddingTop
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libraries.me.Absinthe
import com.absinthe.libraries.utils.utils.UiUtils
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.BarUtils
import com.drakeet.about.*
import com.google.android.material.appbar.AppBarLayout
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AboutActivity : AbsAboutActivity() {

    private var shouldShowEasterEggCount = 1

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        initView()
        Analytics.trackEvent(Constants.Event.SETTINGS, EventProperties().set("PREF_ABOUT", "Entered"))
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateHeader(icon: ImageView, slogan: TextView, version: TextView) {
        icon.load(R.drawable.pic_splash)
        slogan.setText(R.string.app_name)
        version.text = String.format("Version: %s", BuildConfig.VERSION_NAME)
        
        val rebornCoroutine = lifecycleScope.launch(Dispatchers.Default) {
            delay(300)
            shouldShowEasterEggCount = if (slogan.text == "RengeChecker") 11 else 1
        }
        icon.setOnClickListener {       
            if (shouldShowEasterEggCount < 10) {
                rebornCoroutine.cancel()
                shouldShowEasterEggCount++
                rebornCoroutine.start()
            } else if (shouldShowEasterEggCount == 10) {
                slogan.text = "RengeChecker"
                rebornCoroutine.cancel()
                shouldShowEasterEggCount++
                Analytics.trackEvent(Constants.Event.EASTER_EGG, EventProperties().set("EASTER_EGG", "Renge 10 hits"))
            } else {
                if (shouldShowEasterEggCount < 20) {
                    rebornCoroutine.cancel()
                    shouldShowEasterEggCount++
                    rebornCoroutine.start()
                } else if (shouldShowEasterEggCount == 20) {
                    shouldShowEasterEggCount++

                    val inputStream = assets.open("renge.webp")
                    icon.setImageBitmap(BitmapFactory.decodeStream(inputStream))
                    slogan.text = "えい、私もよ。"
                    setHeaderBackground(ColorDrawable(ContextCompat.getColor(this, R.color.renge)))
                    setHeaderContentScrim(ColorDrawable(ContextCompat.getColor(this, R.color.renge)))
                    window.statusBarColor = ContextCompat.getColor(this, R.color.renge)

                    val fd = assets.openFd("renge_no_koe.aac")
                    MediaPlayer().apply {
                        setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                        prepare()
                        start()
                    }
                    Analytics.trackEvent(Constants.Event.EASTER_EGG, EventProperties().set("EASTER_EGG", "Renge 20 hits!"))
                }
            }
        }
    }

    override fun onItemsCreated(items: MutableList<Any>) {

        val hasInstallCoolApk = AppUtils.isAppInstalled(Constants.PACKAGE_NAME_COOLAPK)

        items.apply {
            add(Category("What's this"))
            add(Card(getString(R.string.about_info)))

            add(Category("Developers"))
            val developerUrl = if (hasInstallCoolApk) {
                URLManager.COOLAPK_HOME_PAGE
            } else {
                URLManager.GITHUB_PAGE
            }
            add(Contributor(R.mipmap.pic_rabbit, Absinthe.ME, "Developer & Designer", developerUrl))
            add(Contributor(R.drawable.ic_github, "Source Code", URLManager.GITHUB_REPO_PAGE, URLManager.GITHUB_REPO_PAGE))

            add(Category("Other Works"))
            addAll(Absinthe.getAboutPageRecommendedApps(this@AboutActivity, BuildConfig.APPLICATION_ID))

            val list = listOf(
                "https://www.iconfont.cn/",
                "https://lottiefiles.com/22122-fanimation",
                "https://lottiefiles.com/21836-blast-off",
                "https://lottiefiles.com/1309-smiley-stack",
                "https://lottiefiles.com/7907-3d-camera",
                "https://chojugiga.com/2017/09/05/da4choju53_0031/"
            )
            add(Category("Acknowledgement"))
            add(Card(HtmlCompat.fromHtml(getAcknowledgementHtmlString(list), HtmlCompat.FROM_HTML_MODE_LEGACY)))

            add(Category("Declaration"))
            add(Card(getString(R.string.library_declaration)))

            add(Category("Open Source Licenses"))
            add(License("kotlin", "JetBrains", License.APACHE_2, "https://github.com/JetBrains/kotlin"))
            add(License("MultiType", "drakeet", License.APACHE_2, "https://github.com/drakeet/MultiType"))
            add(License("about-page", "drakeet", License.APACHE_2, "https://github.com/drakeet/about-page"))
            add(License("AndroidX", "Google", License.APACHE_2, "https://source.google.com"))
            add(License("Android Jetpack", "Google", License.APACHE_2, "https://source.google.com"))
            add(License("gson", "Google", License.APACHE_2, "https://github.com/google/gson"))
            add(License("protobuf", "Google", License.APACHE_2, "https://github.com/protocolbuffers/protobuf"))
            add(License("material-components-android", "Google", License.APACHE_2, "https://github.com/material-components/material-components-android"))
            add(License("libraries", "RikkaApps", License.APACHE_2, "https://github.com/RikkaApps/libraries"))
            add(License("MaterialPreference", "RikkaW", License.APACHE_2, "https://github.com/RikkaW/MaterialPreference"))
            add(License("lottie-android", "Airbnb", License.APACHE_2, "https://github.com/airbnb/lottie-android"))
            add(License("MPAndroidChart", "PhilJay", License.APACHE_2, "https://github.com/PhilJay/MPAndroidChart"))
            add(License("Once", "jonfinerty", License.APACHE_2, "https://github.com/jonfinerty/Once"))
            add(License("BaseRecyclerViewAdapterHelper", "CymChad", License.MIT, "https://github.com/CymChad/BaseRecyclerViewAdapterHelper"))
            add(License("AndroidUtilCode", "Blankj", License.APACHE_2, "https://github.com/Blankj/AndroidUtilCode"))
            add(License("OkHttp", "Square", License.APACHE_2, "https://github.com/square/okhttp"))
            add(License("Retrofit", "Square", License.APACHE_2, "https://github.com/square/retrofit"))
            add(License("AndResGuard", "shwenzhang", License.APACHE_2, "https://github.com/shwenzhang/AndResGuard"))
            add(License("apk-parser", "hsiafan", "BSD-2-Clause", "https://github.com/hsiafan/apk-parser"))
            add(License("X2C", "iReaderAndroid", License.MIT, "https://github.com/iReaderAndroid/X2C"))
            add(License("coil", "coil-kt", License.APACHE_2, "https://github.com/coil-kt/coil"))
            add(License("AndroidFastScroll", "zhanghai", License.APACHE_2, "https://github.com/zhanghai/AndroidFastScroll"))
            add(License("AppIconLoader", "zhanghai", License.APACHE_2, "https://github.com/zhanghai/AppIconLoader"))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.about_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.toolbar_rate) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW).apply {
                    data = URLManager.MARKET_PAGE.toUri()
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
                Toasty.showLong(this, R.string.toast_not_existing_market)
            }
        }
        return super.onOptionsItemSelected(menuItem)
    }

    private fun initView() {
        UiUtils.setSystemBarStyle(window)
        findViewById<AppBarLayout>(com.drakeet.about.R.id.header_layout).addPaddingTop(BarUtils.getStatusBarHeight())
    }

    private fun getAcknowledgementHtmlString(list: List<String>): String {
        val sb = StringBuilder()
        val formatItem = "<a href=\"%s\">%s</a><br>"

        sb.append(getString(R.string.resource_declaration)).append("<br>")
        list.forEach { sb.append(String.format(formatItem, it, it)) }
        return sb.toString()
    }
}
