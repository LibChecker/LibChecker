package com.absinthe.libchecker.domain.app.detail.ui

import android.content.pm.ApplicationInfo
import android.text.SpannableStringBuilder
import android.view.View
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.header.AppDetailHeaderExtraInfo
import com.absinthe.libchecker.domain.app.detail.header.AppDetailHeaderTitleData
import com.absinthe.libchecker.domain.app.detail.header.DetailHeaderExtraInfoState
import com.absinthe.libchecker.domain.app.detail.header.DetailHeaderRenderState
import com.absinthe.libchecker.domain.app.detail.ui.view.DetailsTitleView
import com.absinthe.libchecker.utils.extensions.setLongClickCopiedToClipboard
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import me.zhanghai.android.appiconloader.AppIconLoader

class DetailHeaderBinder(
  private val detailsTitleView: DetailsTitleView,
  private val blurView: View,
  private val onAppInfoClick: (String) -> Unit
) {
  private var boundState: DetailHeaderRenderState? = null
  private var boundApplicationInfo: ApplicationInfo? = null

  fun bind(
    state: DetailHeaderRenderState,
    applicationInfo: ApplicationInfo?
  ) {
    val previousState = boundState
    if (previousState?.title != state.title || boundApplicationInfo !== applicationInfo) {
      bindTitle(state.title, applicationInfo)
    }
    if (previousState?.extraInfo != state.extraInfo) {
      bindExtraInfo(state.extraInfo)
    }
    boundState = state
    boundApplicationInfo = applicationInfo
  }

  private fun bindTitle(
    title: AppDetailHeaderTitleData,
    applicationInfo: ApplicationInfo?
  ) {
    detailsTitleView.apply {
      iconView.apply {
        val appIconLoader = AppIconLoader(
          resources.getDimensionPixelSize(R.dimen.lib_detail_icon_size),
          false,
          context
        )
        contentDescription = title.title
        applicationInfo?.let {
          load(appIconLoader.loadIcon(it))
        } ?: run {
          load(R.drawable.ic_icon_blueprint)
        }
        if (title.isAppInfoAvailable) {
          setOnClickListener {
            if (AntiShakeUtils.isInvalidClick(it)) {
              return@setOnClickListener
            }
            onAppInfoClick(title.packageName)
          }
        } else {
          setOnClickListener(null)
        }
        setDetailIconLongClick(applicationInfo, blurView)
      }
      appNameView.apply {
        text = title.appName
        setLongClickCopiedToClipboard(text)
      }
      packageNameView.apply {
        text = title.packageName
        setLongClickCopiedToClipboard(text)
      }
      versionInfoView.apply {
        text = title.versionInfo
        setLongClickCopiedToClipboard(text)
      }
    }
  }

  private fun bindExtraInfo(extraInfo: DetailHeaderExtraInfoState) {
    val text = when (extraInfo) {
      DetailHeaderExtraInfoState.Loading,
      DetailHeaderExtraInfoState.Empty -> ""

      is DetailHeaderExtraInfoState.Android -> formatAndroid(extraInfo.value)

      is DetailHeaderExtraInfoState.Harmony -> formatHarmony(extraInfo)
    }
    detailsTitleView.extraInfoView.apply {
      this.text = text
      setLongClickCopiedToClipboard(text)
    }
  }

  private fun formatAndroid(extraInfo: AppDetailHeaderExtraInfo): CharSequence = buildSpannedString {
    appendLabel("Target: ")
    append(extraInfo.targetSdkInfo)
    appendLabel(" Min: ")
    append(extraInfo.minSdkInfo)
    appendLabel(" Compile: ")
    append(extraInfo.compileSdkInfo)
    appendLabel(" Size: ")
    append(extraInfo.sizeInfo)

    extraInfo.sharedUserId?.let {
      appendLine().append("sharedUserId = $it")
    }
  }

  private fun formatHarmony(extraInfo: DetailHeaderExtraInfoState.Harmony): CharSequence = buildSpannedString {
    appendLabel("Target: ")
    append(extraInfo.targetVersion)
    appendLabel("Min: ")
    append(extraInfo.minSdkVersion)

    extraInfo.jointUserId?.let {
      appendLine().append("jointUserId = $it")
    }
  }

  private fun SpannableStringBuilder.appendLabel(label: String) {
    scale(0.8f) {
      append(label)
    }
  }
}
