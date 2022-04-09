package com.absinthe.libchecker.ui.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.TransitionDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libchecker.utils.showLongToast
import com.absinthe.libraries.me.Absinthe
import com.drakeet.about.AbsAboutActivity
import com.drakeet.about.Card
import com.drakeet.about.Category
import com.drakeet.about.Contributor
import com.drakeet.about.License
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

private const val RENGE_CHECKER = "RengeChecker"

class AboutActivity : AbsAboutActivity() {

  private var shouldShowEasterEggCount = 1
  private val configuration by lazy {
    Configuration(resources.configuration).apply {
      setLocale(
        GlobalValues.locale
      )
    }
  }
  private val mediaPlayer by lazy { MediaPlayer() }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initView()
    Analytics.trackEvent(
      Constants.Event.SETTINGS,
      EventProperties().set("PREF_ABOUT", "Entered")
    )
  }

  override fun onDestroy() {
    super.onDestroy()
    if (shouldShowEasterEggCount == 20) {
      mediaPlayer.release()
    }
  }

  override fun onCreateHeader(icon: ImageView, slogan: TextView, version: TextView) {
    icon.load(R.drawable.pic_logo)
    slogan.setText(R.string.app_name)
    version.text = String.format("Version: %s", BuildConfig.VERSION_NAME)

    val rebornCoroutine = lifecycleScope.launch(Dispatchers.Default) {
      delay(300)
      shouldShowEasterEggCount = if (slogan.text == RENGE_CHECKER) 11 else 1
    }
    icon.setOnClickListener {
      when (shouldShowEasterEggCount) {
        in 0..9 -> {
          rebornCoroutine.cancel()
          shouldShowEasterEggCount++
          rebornCoroutine.start()
        }
        10 -> {
          slogan.text = RENGE_CHECKER
          rebornCoroutine.cancel()
          shouldShowEasterEggCount++
          Analytics.trackEvent(
            Constants.Event.EASTER_EGG,
            EventProperties().set("EASTER_EGG", "Renge 10 hits")
          )
        }
        in 11..19 -> {
          rebornCoroutine.cancel()
          shouldShowEasterEggCount++
          rebornCoroutine.start()
        }
        20 -> {
          rebornCoroutine.cancel()
          shouldShowEasterEggCount++

          val inputStream = assets.open("renge.webp")
          icon.setImageBitmap(BitmapFactory.decodeStream(inputStream))
          slogan.text = "ええ、私もよ。"
          val headerContentLayout =
            findViewById<LinearLayout>(com.drakeet.about.R.id.header_content_layout)
          val drawable = TransitionDrawable(
            arrayOf(
              headerContentLayout.background,
              ColorDrawable(R.color.renge.getColor(this))
            )
          )
          setHeaderBackground(drawable)
          setHeaderContentScrim(ColorDrawable(R.color.renge.getColor(this)))
          window.statusBarColor = R.color.renge.getColor(this)
          drawable.startTransition(250)

          val fd = assets.openFd("renge_no_koe.aac")
          mediaPlayer.also {
            it.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            it.prepare()
            it.start()
          }
          GlobalValues.rengeTheme = !GlobalValues.rengeTheme
          Analytics.trackEvent(
            Constants.Event.EASTER_EGG,
            EventProperties().set("EASTER_EGG", "Renge 20 hits!")
          )
        }
      }
    }
  }

  override fun onItemsCreated(items: MutableList<Any>) {

    val hasInstallCoolApk = PackageUtils.isAppInstalled(Constants.PACKAGE_NAME_COOLAPK)

    items.apply {
      add(Category("What's this"))
      add(Card(getStringByConfiguration(R.string.about_info)))

      add(Category("Developers"))
      val developerUrl = if (hasInstallCoolApk) {
        URLManager.COOLAPK_HOME_PAGE
      } else {
        URLManager.GITHUB_PAGE
      }
      add(Contributor(R.drawable.pic_rabbit, Absinthe.ME, "Developer & Designer", developerUrl))
      add(Contributor(R.drawable.pic_kali, "Goooler", "Code Tidy & Optimize", "https://github.com/Goooler"))
      add(
        Contributor(
          R.drawable.ic_github,
          "Source Code",
          URLManager.GITHUB_REPO_PAGE,
          URLManager.GITHUB_REPO_PAGE
        )
      )

      add(Category("Other Works"))
      addAll(
        Absinthe.getAboutPageRecommendedApps(
          this@AboutActivity,
          BuildConfig.APPLICATION_ID
        )
      )

      add(Category("Contribution"))
      add(
        Contributor(
          0/*TODO*/,
          "Telegram @tommynok",
          "Russian & Ukrainian Translation",
          "https://t.me/tommynok"
        )
      )

      val list = listOf(
        "https://www.iconfont.cn/",
        "https://lottiefiles.com/22122-fanimation",
        "https://lottiefiles.com/21836-blast-off",
        "https://lottiefiles.com/1309-smiley-stack",
        "https://lottiefiles.com/44836-gray-down-arrow",
        "https://lottiefiles.com/66818-holographic-radar",
        "https://chojugiga.com/2017/09/05/da4choju53_0031/"
      )
      add(Category("Acknowledgement"))
      add(
        Card(
          HtmlCompat.fromHtml(
            getAcknowledgementHtmlString(list),
            HtmlCompat.FROM_HTML_MODE_LEGACY
          )
        )
      )

      add(Category("Declaration"))
      add(Card(getStringByConfiguration(R.string.library_declaration)))

      add(Category("Open Source Licenses"))
      add(
        License(
          "kotlin",
          "JetBrains",
          License.APACHE_2,
          "https://github.com/JetBrains/kotlin"
        )
      )
      add(
        License(
          "MultiType",
          "drakeet",
          License.APACHE_2,
          "https://github.com/drakeet/MultiType"
        )
      )
      add(
        License(
          "about-page",
          "drakeet",
          License.APACHE_2,
          "https://github.com/drakeet/about-page"
        )
      )
      add(
        License(
          "AndroidX",
          "Google",
          License.APACHE_2,
          "https://source.google.com"
        )
      )
      add(
        License(
          "Android Jetpack",
          "Google",
          License.APACHE_2,
          "https://source.google.com"
        )
      )
      add(
        License(
          "protobuf",
          "Google",
          License.APACHE_2,
          "https://github.com/protocolbuffers/protobuf"
        )
      )
      add(
        License(
          "material-components-android",
          "Google",
          License.APACHE_2,
          "https://github.com/material-components/material-components-android"
        )
      )
      add(
        License(
          "RikkaX",
          "RikkaApps",
          License.MIT,
          "https://github.com/RikkaApps/RikkaX"
        )
      )
      add(
        License(
          "HiddenApiRefinePlugin",
          "RikkaApps",
          License.MIT,
          "https://github.com/RikkaApps/HiddenApiRefinePlugin"
        )
      )
      add(
        License(
          "lottie-android",
          "Airbnb",
          License.APACHE_2,
          "https://github.com/airbnb/lottie-android"
        )
      )
      add(
        License(
          "MPAndroidChart",
          "PhilJay",
          License.APACHE_2,
          "https://github.com/PhilJay/MPAndroidChart"
        )
      )
      add(
        License(
          "Once",
          "jonfinerty",
          License.APACHE_2,
          "https://github.com/jonfinerty/Once"
        )
      )
      add(
        License(
          "BaseRecyclerViewAdapterHelper",
          "CymChad",
          License.MIT,
          "https://github.com/CymChad/BaseRecyclerViewAdapterHelper"
        )
      )
      add(
        License(
          "OkHttp",
          "Square",
          License.APACHE_2,
          "https://github.com/square/okhttp"
        )
      )
      add(
        License(
          "Retrofit",
          "Square",
          License.APACHE_2,
          "https://github.com/square/retrofit"
        )
      )
      add(
        License(
          "Moshi",
          "Square",
          License.APACHE_2,
          "https://github.com/square/moshi"
        )
      )
      add(
        License(
          "dexlib2",
          "JesusFreke",
          License.APACHE_2,
          "https://github.com/JesusFreke/smali"
        )
      )
      add(
        License(
          "coil",
          "coil-kt",
          License.APACHE_2,
          "https://github.com/coil-kt/coil"
        )
      )
      add(
        License(
          "AndroidFastScroll",
          "zhanghai",
          License.APACHE_2,
          "https://github.com/zhanghai/AndroidFastScroll"
        )
      )
      add(
        License(
          "AppIconLoader",
          "zhanghai",
          License.APACHE_2,
          "https://github.com/zhanghai/AppIconLoader"
        )
      )
      add(
        License(
          "LSPosed",
          "LSPosed",
          License.GPL_V3,
          "https://github.com/LSPosed/LSPosed"
        )
      )
      add(
        License(
          "AndroidHiddenApiBypass",
          "LSPosed",
          License.APACHE_2,
          "https://github.com/LSPosed/AndroidHiddenApiBypass"
        )
      )
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.about_menu, menu)
    return true
  }

  override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
    if (menuItem.itemId == R.id.toolbar_rate) {
      try {
        startActivity(
          Intent(Intent.ACTION_VIEW)
            .setData(URLManager.MARKET_PAGE.toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
      } catch (e: ActivityNotFoundException) {
        Timber.e(e)
        showLongToast(R.string.toast_not_existing_market)
      }
    }
    return super.onOptionsItemSelected(menuItem)
  }

  private fun initView() {
    findViewById<Toolbar>(R.id.toolbar)?.background = null
    val color = getColor(R.color.aboutHeader)
    setHeaderBackground(ColorDrawable(color))
    setHeaderContentScrim(ColorDrawable(color))
  }

  private fun getAcknowledgementHtmlString(list: List<String>): String {
    val sb = StringBuilder()
    val formatItem = "<a href=\"%s\">%s</a><br>"

    sb.append(getStringByConfiguration(R.string.resource_declaration)).append("<br>")
    list.forEach { sb.append(String.format(formatItem, it, it)) }
    return sb.toString()
  }

  private fun getStringByConfiguration(@StringRes res: Int): String =
    createConfigurationContext(configuration).resources.getString(res)
}
