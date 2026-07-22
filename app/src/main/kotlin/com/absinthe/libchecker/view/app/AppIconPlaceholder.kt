package com.absinthe.libchecker.view.app

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import me.zhanghai.android.appiconloader.AppIconLoader

object AppIconPlaceholder {

  @get:DrawableRes
  val resourceId: Int
    get() = R.mipmap.ic_app_icon_placeholder

  fun newDrawable(context: Context): Drawable? {
    val applicationInfo = ApplicationInfo(context.applicationInfo).apply {
      icon = resourceId
    }
    val bitmap = AppIconLoader(
      context.getDimensionPixelSize(R.dimen.app_icon_size),
      false,
      context
    ).loadIcon(applicationInfo)
    return BitmapDrawable(context.resources, bitmap)
  }
}
