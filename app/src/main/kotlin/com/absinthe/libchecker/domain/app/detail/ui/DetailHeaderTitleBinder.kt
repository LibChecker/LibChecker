package com.absinthe.libchecker.domain.app.detail.ui

import android.content.pm.ApplicationInfo
import android.view.View
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.header.AppDetailHeaderTitleData
import com.absinthe.libchecker.domain.app.detail.ui.view.DetailsTitleView
import com.absinthe.libchecker.utils.extensions.setLongClickCopiedToClipboard
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import me.zhanghai.android.appiconloader.AppIconLoader

class DetailHeaderTitleBinder(
  private val detailsTitleView: DetailsTitleView,
  private val blurView: View,
  private val onAppInfoClick: (String) -> Unit
) {
  fun bind(
    headerTitleData: AppDetailHeaderTitleData,
    applicationInfo: ApplicationInfo?
  ) {
    detailsTitleView.apply {
      iconView.apply {
        val appIconLoader = AppIconLoader(
          resources.getDimensionPixelSize(R.dimen.lib_detail_icon_size),
          false,
          context
        )
        contentDescription = headerTitleData.title
        applicationInfo?.let {
          load(appIconLoader.loadIcon(it))
        } ?: run {
          load(R.drawable.ic_icon_blueprint)
        }
        if (headerTitleData.isAppInfoAvailable) {
          setOnClickListener {
            if (AntiShakeUtils.isInvalidClick(it)) {
              return@setOnClickListener
            }
            onAppInfoClick(headerTitleData.packageName)
          }
        } else {
          setOnClickListener(null)
        }
        setDetailIconLongClick(applicationInfo, blurView)
      }
      appNameView.apply {
        text = headerTitleData.appName
        setLongClickCopiedToClipboard(text)
      }
      packageNameView.apply {
        text = headerTitleData.packageName
        setLongClickCopiedToClipboard(text)
      }
      versionInfoView.apply {
        text = headerTitleData.versionInfo
        setLongClickCopiedToClipboard(text)
      }
    }
  }
}
