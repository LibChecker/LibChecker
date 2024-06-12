package com.absinthe.libchecker.about

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.lang.ref.WeakReference

class Renge(private val contextRef: WeakReference<Context>) {

  val renge: Bitmap? = contextRef.get()?.let {
    BitmapFactory.decodeStream(it.assets.open("easter_egg/renge.webp"))
  }

  fun inori() {

  }

  fun sayonara() {

  }
}
