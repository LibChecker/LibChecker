package com.absinthe.libchecker.features.applist.detail.ui

import android.text.SpannableStringBuilder
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import com.absinthe.libchecker.domain.app.detail.AppDetailHeaderExtraInfo
import com.absinthe.libchecker.features.applist.detail.ui.view.DetailsTitleView
import com.absinthe.libchecker.utils.extensions.setLongClickCopiedToClipboard
import ohos.bundle.BundleInfo

class DetailHeaderExtraInfoBinder(
  private val detailsTitleView: DetailsTitleView
) {
  fun format(headerExtraInfo: AppDetailHeaderExtraInfo): CharSequence = buildSpannedString {
    appendLabel("Target: ")
    append(headerExtraInfo.targetSdkInfo)
    appendLabel(" Min: ")
    append(headerExtraInfo.minSdkInfo)
    appendLabel(" Compile: ")
    append(headerExtraInfo.compileSdkInfo)
    appendLabel(" Size: ")
    append(headerExtraInfo.sizeInfo)

    headerExtraInfo.sharedUserId?.let {
      appendLine().append("sharedUserId = $it")
    }
  }

  fun formatHarmony(hapBundle: BundleInfo?): CharSequence = buildSpannedString {
    hapBundle?.let {
      appendLabel("Target: ")
      append(it.targetVersion.toString())
      appendLabel("Min: ")
      append(it.minSdkVersion.toString())

      if (!it.jointUserId.isNullOrEmpty()) {
        appendLine().append("jointUserId = ${it.jointUserId}")
      }
    }
  }

  fun bind(versionInfo: CharSequence) {
    detailsTitleView.extraInfoView.apply {
      text = versionInfo
      setLongClickCopiedToClipboard(text)
    }
  }

  private fun SpannableStringBuilder.appendLabel(label: String) {
    scale(0.8f) {
      append(label)
    }
  }
}
