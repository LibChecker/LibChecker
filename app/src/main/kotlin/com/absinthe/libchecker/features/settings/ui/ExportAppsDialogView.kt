package com.absinthe.libchecker.features.settings.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.core.view.isVisible
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.manager.SystemBarManager
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import timber.log.Timber

class ExportAppsDialogView(context: Context) :
  LinearLayout(context),
  IHeaderView {

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.export_apps)
  }

  private val description = AppCompatTextView(context).apply {
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
      topMargin = 8.dp
      leftMargin = 16.dp
      rightMargin = 16.dp
    }
    gravity = Gravity.CENTER
    text = buildDescriptionText(context)
    movementMethod = LinkMovementMethod.getInstance()
    linksClickable = true
  }

  private val progressIndicator = LinearProgressIndicator(
    ContextThemeWrapper(context, R.style.App_Widget_M3E_LinearProgressIndicator_Wavy)
  ).apply {
    layoutParams = LayoutParams(300.dp, LayoutParams.WRAP_CONTENT).apply {
      topMargin = 14.dp
    }
    max = 100
    progress = 0
    isIndeterminate = false
    isVisible = false
  }

  private val webUiPreview = AppCompatImageView(context).apply {
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 170.dp).apply {
      topMargin = 20.dp
      leftMargin = 16.dp
      rightMargin = 16.dp
    }
    scaleType = ImageView.ScaleType.FIT_CENTER
    adjustViewBounds = true
    setImageResource(R.drawable.ic_webui_skeleton_preview)
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
  }

  val exportButton = MaterialButton(context).apply {
    layoutParams = LayoutParams(300.dp, LayoutParams.WRAP_CONTENT).apply {
      topMargin = 16.dp
    }
    setText(R.string.export_apps_start)
  }

  init {
    orientation = VERTICAL
    gravity = Gravity.CENTER_HORIZONTAL
    val padding = 16.dp
    setPadding(
      padding,
      padding,
      padding,
      (padding - SystemBarManager.navigationBarSize).coerceAtLeast(0)
    )
    addView(header)
    addView(description)
    addView(webUiPreview)
    addView(progressIndicator)
    addView(exportButton)
  }

  fun showReady() {
    progressIndicator.isVisible = false
    progressIndicator.progress = 0
    exportButton.isEnabled = true
    exportButton.setText(R.string.export_apps_start)
  }

  fun showPreparing() {
    progressIndicator.isVisible = false
    exportButton.isEnabled = false
  }

  fun showExporting() {
    progressIndicator.isVisible = true
    progressIndicator.setProgressCompat(0, false)
    exportButton.isEnabled = false
  }

  fun showDone() {
    progressIndicator.isVisible = true
    progressIndicator.setProgressCompat(100, true)
    exportButton.isEnabled = true
    exportButton.setText(android.R.string.ok)
  }

  fun setProgress(progress: Int) {
    progressIndicator.setProgressCompat(progress.coerceIn(0, 100), true)
  }

  override fun getHeaderView(): BottomSheetHeaderView = header

  private fun buildDescriptionText(context: Context): SpannableString {
    val text = context.getString(R.string.export_apps_webui_tip)
    val span = SpannableString(text)
    val start = text.indexOf(WEBUI_TEXT)
    if (start >= 0) {
      span.setSpan(
        object : ClickableSpan() {
          override fun onClick(widget: View) {
            openWebUi(widget.context)
          }
        },
        start,
        start + WEBUI_TEXT.length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
      )
    }
    return span
  }

  private fun openWebUi(context: Context) {
    runCatching {
      CustomTabsIntent.Builder().build().launchUrl(context, URLManager.WEBUI_PAGE.toUri())
    }.onFailure {
      Timber.e(it)
      runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, URLManager.WEBUI_PAGE.toUri()))
      }.onFailure { inner ->
        Timber.e(inner)
        if (inner is ActivityNotFoundException) {
          Toasty.showShort(context, "No browser application")
        }
      }
    }
  }

  private companion object {
    const val WEBUI_TEXT = "WebUI"
  }
}
