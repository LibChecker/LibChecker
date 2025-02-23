package com.absinthe.libchecker.ui.base

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import coil.size.Size
import coil.transform.Transformation

class SaturationTransformation(private val saturation: Float) : Transformation {

  override val cacheKey: String
    get() = "SaturationTransformation(saturation=$saturation)"

  override suspend fun transform(input: Bitmap, size: Size): Bitmap {
    val width = input.width
    val height = input.height
    val output = createBitmap(width, height, input.config ?: Bitmap.Config.ARGB_8888)

    val canvas = Canvas(output)
    val paint = Paint()
    val colorMatrix = ColorMatrix()
    colorMatrix.setSaturation(saturation)
    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    canvas.drawBitmap(input, 0f, 0f, paint)

    input.recycle()
    return output
  }
}
