package com.absinthe.libchecker.domain.settings.ui

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.domain.settings.model.ExportAppsDialogAction
import com.absinthe.libchecker.domain.settings.model.ExportAppsDialogState
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.openUrlInBrowser
import com.absinthe.libchecker.view.app.BottomSheetScaffoldView
import com.absinthe.libraries.utils.manager.SystemBarManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator

class ExportAppsDialogView(context: Context) : BottomSheetScaffoldView(context) {

  private var onAction: (ExportAppsDialogAction) -> Unit = {}

  private val description = AppCompatTextView(context).apply {
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
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

  private val exportButton = MaterialButton(context).apply {
    layoutParams = LayoutParams(300.dp, LayoutParams.WRAP_CONTENT).apply {
      topMargin = 16.dp
    }
    setText(R.string.export_apps_start)
  }

  init {
    gravity = Gravity.CENTER_HORIZONTAL
    val padding = 16.dp
    setPadding(
      padding,
      padding,
      padding,
      (padding - SystemBarManager.navigationBarSize).coerceAtLeast(0)
    )
    header.title.text = context.getString(R.string.export_apps)
    addView(description)
    addView(webUiPreview)
    addView(progressIndicator)
    addView(exportButton)
    exportButton.setOnClickListener {
      onAction(ExportAppsDialogAction.PrimaryButtonClick)
    }
  }

  fun bind(
    state: ExportAppsDialogState,
    onAction: (ExportAppsDialogAction) -> Unit
  ) {
    this.onAction = onAction
    when (state) {
      ExportAppsDialogState.Ready -> {
        progressIndicator.isVisible = false
        progressIndicator.progress = 0
        exportButton.isEnabled = true
        exportButton.setText(R.string.export_apps_start)
      }

      ExportAppsDialogState.Preparing -> {
        progressIndicator.isVisible = false
        exportButton.isEnabled = false
        exportButton.setText(R.string.export_apps_start)
      }

      is ExportAppsDialogState.Exporting -> {
        progressIndicator.isVisible = true
        val progress = state.progress.coerceIn(0, 100)
        progressIndicator.setProgressCompat(progress, progress > 0)
        exportButton.isEnabled = false
        exportButton.setText(R.string.export_apps_start)
      }

      ExportAppsDialogState.Done -> {
        progressIndicator.isVisible = true
        progressIndicator.setProgressCompat(100, true)
        exportButton.isEnabled = true
        exportButton.setText(android.R.string.ok)
      }
    }
  }

  private fun buildDescriptionText(context: Context): SpannableString {
    val text = context.getString(R.string.export_apps_webui_tip)
    val span = SpannableString(text)
    val start = text.indexOf(WEBUI_TEXT)
    if (start >= 0) {
      span.setSpan(
        object : ClickableSpan() {
          override fun onClick(widget: View) {
            widget.context.openUrlInBrowser(URLManager.WEBUI_PAGE)
          }
        },
        start,
        start + WEBUI_TEXT.length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
      )
    }
    return span
  }

  private companion object {
    const val WEBUI_TEXT = "WebUI"
  }
}
