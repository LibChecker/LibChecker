package com.absinthe.libchecker.features.about

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.findFragment
import com.absinthe.libchecker.R
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.mikepenz.aboutlibraries.LibsBuilder
import com.mikepenz.aboutlibraries.LibsConfiguration
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.util.SpecialButton
import timber.log.Timber

object AboutPageBuilder {
  fun start(context: Context) {
    LibsBuilder()
      .withAboutIconShown(true)
      .withAboutVersionShown(true)
      .withAboutDescription(context.getString(R.string.about_info))
      .withActivityTitle(context.getString(R.string.settings_about))
      .withAboutSpecial1("Devs")
      .withAboutSpecial2("Contribs")
      .withAboutSpecial3("Credits")
      .withListener(object : LibsConfiguration.LibsListener {
        override fun onIconClicked(v: View) {
        }

        override fun onLibraryAuthorClicked(
          v: View,
          library: Library
        ): Boolean {
          return false
        }

        override fun onLibraryContentClicked(
          v: View,
          library: Library
        ): Boolean {
          return false
        }

        override fun onLibraryBottomClicked(
          v: View,
          library: Library
        ): Boolean {
          return false
        }

        override fun onExtraClicked(
          v: View,
          specialButton: SpecialButton
        ): Boolean {
          val context = v.context
          when (specialButton) {
            SpecialButton.SPECIAL1 -> {
              Timber.d("Special1 clicked: ${getFragmentManager(context)}")
              v.findFragment<Fragment>().let {
                DevelopersDialogFragment().show(
                  it.childFragmentManager,
                  DevelopersDialogFragment::class.java.name
                )
              }
            }

            SpecialButton.SPECIAL2 -> {
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
              BaseAlertDialogBuilder(context)
                .setIcon(R.drawable.ic_team)
                .setTitle("Contributors")
                .setMessage(
                  HtmlCompat.fromHtml(
                    contributors.toString(),
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                  )
                )
                .setPositiveButton(android.R.string.ok, null)
                .show()
            }

            SpecialButton.SPECIAL3 -> {
              val content = StringBuilder()
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
              content.append("<b>").append("Acknowledgement").append("</b>").append("<br>")
              content.append(getAcknowledgementHtmlString(context, list)).append("<br>")
              content.append("<b>").append("Declaration").append("</b>").append("<br>")
              content.append(context.getString(R.string.library_declaration)).append("<br>")
                .append("<br>")
              content.append("<b>").append("Privacy Policy").append("</b>").append("<br>")
              content.append(getHyperLink("https://absinthe.life/LibChecker-Docs/guide/PRIVACY/"))

              BaseAlertDialogBuilder(context)
                .setIcon(R.drawable.ic_content)
                .setTitle("Credits")
                .setMessage(
                  HtmlCompat.fromHtml(
                    content.toString(),
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                  )
                )
                .setPositiveButton(android.R.string.ok, null)
                .show()
            }
          }
          return true
        }

        override fun onIconLongClicked(v: View): Boolean {
          return false
        }

        override fun onLibraryAuthorLongClicked(
          v: View,
          library: Library
        ): Boolean {
          return false
        }

        override fun onLibraryContentLongClicked(
          v: View,
          library: Library
        ): Boolean {
          return false
        }

        override fun onLibraryBottomLongClicked(
          v: View,
          library: Library
        ): Boolean {
          return false
        }
      })
      .start(context)
  }

  private fun getHyperLink(url: String): String {
    return String.format("<a href=\"%s\">%s</a>", url, url)
  }

  private fun getAcknowledgementHtmlString(context: Context, list: List<String>): String {
    val sb = StringBuilder()

    sb.append(context.getString(R.string.resource_declaration)).append("<br>")
    list.forEach { sb.append(getHyperLink(it)).append("<br>") }
    return sb.toString()
  }

  private fun getFragmentManager(context: Context?): FragmentManager? {
    Timber.d("Context: $context")
    return when (context) {
      is AppCompatActivity -> context.supportFragmentManager
      is ContextThemeWrapper -> getFragmentManager(context.baseContext)
      else -> null
    }
  }
}
