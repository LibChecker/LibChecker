package com.absinthe.libchecker.view.app

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getDrawable

object AppIconPlaceholder {

  @get:DrawableRes
  val resourceId: Int
    get() = R.mipmap.ic_app_icon_placeholder

  fun newDrawable(context: Context): Drawable? {
    return resourceId.getDrawable(context)
  }
}
