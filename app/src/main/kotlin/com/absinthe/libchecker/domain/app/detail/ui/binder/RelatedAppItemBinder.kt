package com.absinthe.libchecker.domain.app.detail.ui.binder

import android.text.SpannableString
import android.text.style.ImageSpan
import androidx.core.view.isVisible
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.related.RelatedAppDisplayData
import com.absinthe.libchecker.domain.app.list.ui.view.AppItemView
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.view.span.CenterAlignImageSpan

class RelatedAppItemBinder {

  fun bind(
    appItemView: AppItemView,
    title: CharSequence,
    data: RelatedAppDisplayData,
    showHarmonyBadge: Boolean = data.isHarmony,
    onClick: () -> Unit
  ) {
    appItemView.container.also {
      it.icon.load(data.packageInfo)
      it.appName.text = data.label
      it.packageName.text = data.packageName
      it.versionInfo.text = data.versionInfo
      it.abiInfo.text = buildAbiInfo(appItemView, data)
      it.abiInfo.isVisible = true
      if (showHarmonyBadge) {
        it.setBadge(R.drawable.ic_harmony_badge)
      } else {
        it.setBadge(null)
      }
    }

    appItemView.setItemContentDescription(
      title,
      data.label,
      data.packageName,
      appItemView.container.versionInfo.text,
      data.abiInfo
    )
    appItemView.setOnClickListener { onClick() }
  }

  private fun buildAbiInfo(
    appItemView: AppItemView,
    data: RelatedAppDisplayData
  ): CharSequence {
    val badgeRes = data.abiBadgeRes ?: return data.abiInfo
    val spanString = SpannableString("  ${data.abiInfo}")
    badgeRes.getDrawable(appItemView.context)?.let { drawable ->
      drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
      val span = CenterAlignImageSpan(drawable)
      spanString.setSpan(span, 0, 1, ImageSpan.ALIGN_BOTTOM)
    }
    return spanString
  }
}
