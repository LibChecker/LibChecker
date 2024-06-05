package com.absinthe.libchecker.features.about.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.TransitionDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import coil.load
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.about.Renge
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.features.about.AbsAboutActivityProxy
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libchecker.utils.showLongToast
import com.absinthe.libraries.me.Absinthe
import com.drakeet.about.Card
import com.drakeet.about.Category
import com.drakeet.about.Contributor
import com.drakeet.about.License
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import java.lang.ref.WeakReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

private const val RENGE_CHECKER = "RengeChecker"

class AboutActivity :
  AbsAboutActivityProxy(),
  MenuProvider {

  private var shouldShowEasterEggCount = 1
  private val configuration by lazy {
    Configuration(resources.configuration).apply {
      setLocale(
        GlobalValues.locale
      )
    }
  }
  private val renge by lazy { Renge(WeakReference(this)) }

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
      renge.sayonara()
    }
  }

  override fun onCreateHeader(icon: ImageView, slogan: TextView, version: TextView) {
    icon.load(R.drawable.pic_logo)
    slogan.setText(R.string.app_name)
    if (GlobalValues.debugMode) {
      version.text = String.format("Build Time: %s", BuildConfig.BUILD_TIME)
    } else {
      version.text = String.format("Version: %s", BuildConfig.VERSION_NAME)
    }

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

          renge.renge?.let {
            icon.setImageBitmap(it)
          }
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

          renge.inori()
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
    items.apply {
      add(Category("What's this"))
      add(Card(getStringByConfiguration(R.string.about_info)))

      add(Category("Developers"))
      add(
        Contributor(
          R.drawable.pic_rabbit,
          Absinthe.ME,
          "Developer & Designer",
          Absinthe.GITHUB_HOME_PAGE
        )
      )
      add(
        Contributor(
          R.drawable.pic_kali,
          "Goooler",
          "Code Tidy & Optimize",
          "https://github.com/Goooler"
        )
      )
      add(
        Contributor(
          R.drawable.pic_qhy040404,
          "qhy040404",
          "Developer",
          "https://github.com/qhy040404"
        )
      )
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
      val contributors = StringBuilder()
      contributors.append("Russian & Ukrainian Translation: ")
        .append("<b>")
        .append("tommynok")
        .append("</b>")
        .append("[")
        .append(getHyperLink("https://t.me/tommynok"))
        .append("]")
        .append("<br>")
      contributors.append("Harmony OS detection methods: ")
        .append("<b>")
        .append("su1216")
        .append("</b>")
        .append("[")
        .append(getHyperLink("https://t.me/dear_su1216"))
        .append("]")
        .append("<br>")
      contributors.append("Bug Reporter: ")
        .append("<b>")
        .append("LiuXing")
        .append("</b>")
        .append("[")
        .append(getHyperLink("https://www.coolapk.com/u/1382006"))
        .append("]")
        .append("<br>")
      contributors.append("Bug Reporter: ")
        .append("<b>")
        .append("Flyzc")
        .append("</b>")
        .append("[")
        .append(getHyperLink("https://t.me/Flyzc"))
        .append("]")
      add(
        Card(
          HtmlCompat.fromHtml(
            contributors.toString(),
            HtmlCompat.FROM_HTML_MODE_LEGACY
          )
        )
      )

      val list = listOf(
        "https://www.iconfont.cn/",
        "https://lottiefiles.com/22122-fanimation",
        "https://lottiefiles.com/77311-sweet-teapot-with-autumn-herbs-and-birds",
        "https://lottiefiles.com/51686-a-botanical-wreath-loading",
        "https://lottiefiles.com/21836-blast-off",
        "https://lottiefiles.com/1309-smiley-stack",
        "https://lottiefiles.com/44836-gray-down-arrow",
        "https://lottiefiles.com/66818-holographic-radar",
        "https://pictogrammers.com/library/mdi/"
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

      add(Category("Privacy Policy"))
      add(Card("https://absinthe.life/LibChecker-Docs/guide/PRIVACY/"))

      add(Category("Open Source Licenses"))
      add(
        License(
          "LibChecker-Rules-Bundle",
          "LibChecker",
          License.APACHE_2,
          "https://github.com/LibChecker/LibChecker-Rules-Bundle"
        )
      )
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
          "Google",
          License.APACHE_2,
          "https://github.com/google/smali"
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
      add(
        License(
          "cascade",
          "saket",
          License.APACHE_2,
          "https://github.com/saket/cascade"
        )
      )
      add(
        License(
          "Android-Room-Database-Backup",
          "rafi0101",
          License.MIT,
          "https://github.com/rafi0101/Android-Room-Database-Backup"
        )
      )
    }
  }

  override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
    menuInflater.inflate(R.menu.about_menu, menu)
  }

  override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
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
    return true
  }

  private fun initView() {
    addMenuProvider(this, this, Lifecycle.State.STARTED)
    findViewById<Toolbar>(R.id.toolbar)?.let {
      it.title = getString(R.string.settings_about)
    }
    val color = getColor(R.color.aboutHeader)
    setHeaderBackground(ColorDrawable(color))
    setHeaderContentScrim(ColorDrawable(color))
  }

  private fun getAcknowledgementHtmlString(list: List<String>): String {
    val sb = StringBuilder()

    sb.append(getStringByConfiguration(R.string.resource_declaration)).append("<br>")
    list.forEach { sb.append(getHyperLink(it)).append("<br>") }
    return sb.toString()
  }

  private fun getHyperLink(url: String): String {
    return String.format("<a href=\"%s\">%s</a>", url, url)
  }

  private fun getStringByConfiguration(@StringRes res: Int): String =
    createConfigurationContext(configuration).resources.getString(res)
}
