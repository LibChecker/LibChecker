package com.absinthe.libchecker.about

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import java.lang.ref.WeakReference

class Renge(private val contextRef: WeakReference<Context>) {
  private val mp = MediaPlayer()

  val renge: Bitmap? = contextRef.get()?.let {
    BitmapFactory.decodeStream(it.assets.open("renge.webp"))
  }

  fun inori() {
    contextRef.get()?.let { context ->
      val fd = context.assets.openFd("renge_no_koe.aac")
      mp.also {
        it.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
        it.prepare()
        it.start()
      }
    }
  }

  fun sayonara() {
    mp.stop()
    mp.release()
  }
}
