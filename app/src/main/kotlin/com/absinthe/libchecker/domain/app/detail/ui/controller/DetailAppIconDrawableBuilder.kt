package com.absinthe.libchecker.domain.app.detail.ui.controller

import android.content.Context
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.RequiresApi
import com.absinthe.libchecker.domain.app.detail.model.AppIconItem
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.getColorByAttr

class DetailAppIconDrawableBuilder(
  private val context: Context
) {

  fun build(appIcons: List<AppIconItem>): List<Drawable> {
    val firstIcon = appIcons[0]
    val drawables = appIcons.map { it.drawable }.toMutableList()

    val processedDrawable = when {
      OsUtils.atLeastT() && firstIcon.isMonochrome && firstIcon.drawable is AdaptiveIconDrawable -> {
        createMonochromeIconWithBackground(firstIcon.drawable)
      }

      else -> firstIcon.drawable
    }

    processedDrawable?.let {
      drawables[0] = it
    } ?: drawables.removeAt(0)

    return drawables
  }

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  private fun createMonochromeIconWithBackground(adaptiveIcon: AdaptiveIconDrawable): Drawable? {
    return adaptiveIcon.monochrome?.apply {
      setTint(context.getColorByAttr(androidx.appcompat.R.attr.colorPrimary))
    }?.let { foreground ->
      UiUtils.addCircleBackground(
        context,
        foreground,
        context.getColorByAttr(com.google.android.material.R.attr.colorSecondaryContainer)
      )
    }
  }
}
