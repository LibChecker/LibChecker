package com.absinthe.libchecker.compat

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.graphics.withSave

/** Draws a backdrop filter before foreground content, using the caller's canvas clip. */
@RequiresApi(33)
class BackdropBlurCompat {
  private var node: RenderNode? = null
  private var radius = Float.NaN

  @SuppressLint("NewApi")
  fun draw(canvas: Canvas, left: Float, top: Float, width: Int, height: Int, radiusPx: Float): Boolean {
    if (!isSupported || !canvas.isHardwareAccelerated || width <= 0 || height <= 0) return false
    val backdrop = node ?: RenderNode("LC.Navigation.backdrop").also { node = it }
    if (backdrop.width != width || backdrop.height != height || !backdrop.hasDisplayList()) {
      backdrop.setPosition(0, 0, width, height)
      backdrop.beginRecording(width, height).drawColor(Color.TRANSPARENT)
      backdrop.endRecording()
    }
    if (radius != radiusPx) {
      backdrop.setBackdropRenderEffect(RenderEffect.createBlurEffect(radiusPx, radiusPx, Shader.TileMode.CLAMP))
      radius = radiusPx
    }
    canvas.withSave {
      translate(left, top)
      drawRenderNode(backdrop)
    }
    return true
  }

  fun clear() {
    node?.discardDisplayList()
    node = null
    radius = Float.NaN
  }

  companion object {
    val isSupported: Boolean
      get() = Build.VERSION.SDK_INT >= 36 &&
        Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.CINNAMON_BUN_2
  }
}
