package com.absinthe.libchecker.view.app

import android.widget.ImageView
import android.widget.TextView
import com.absinthe.libchecker.utils.extensions.setLongClickCopiedToClipboard

internal data class AppIdentityHeaderRenderState(
  val appName: CharSequence?,
  val iconContentDescription: CharSequence,
  val packageName: CharSequence,
  val versionInfo: CharSequence,
  val copyPrimaryText: Boolean = true
)

internal class AppIdentityHeaderRenderer(
  private val iconView: ImageView,
  private val appNameView: TextView,
  private val packageNameView: TextView,
  private val versionInfoView: TextView,
  private val setVersionInfo: (CharSequence) -> Unit = versionInfoView::setText
) {
  fun render(state: AppIdentityHeaderRenderState) {
    iconView.contentDescription = state.iconContentDescription
    appNameView.apply {
      text = state.appName
      if (state.copyPrimaryText) {
        setLongClickCopiedToClipboard(text)
      }
    }
    packageNameView.apply {
      text = state.packageName
      if (state.copyPrimaryText) {
        setLongClickCopiedToClipboard(text)
      }
    }
    setVersionInfo(state.versionInfo)
    if (state.copyPrimaryText) {
      versionInfoView.setLongClickCopiedToClipboard(state.versionInfo)
    }
  }
}
