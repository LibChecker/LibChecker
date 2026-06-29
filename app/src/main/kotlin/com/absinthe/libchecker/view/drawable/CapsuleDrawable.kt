package com.absinthe.libchecker.view.drawable

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.withTranslation

class CapsuleDrawable(
  private val context: Context,
  private val text: String,
  private val textSize: Float = 40f,
  private val textColor: Int = Color.BLACK,
  private val backgroundColor: Int = Color.WHITE,
  private val borderColor: Int = Color.BLACK,
  private val borderWidth: Float = 1f,
  private val cornerRadius: Float = 50f
) : Drawable() {

  private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
    this.textSize = this@CapsuleDrawable.textSize
    this.color = textColor
    this.typeface = Typeface.DEFAULT_BOLD
  }

  private val capsuleDrawable = GradientDrawable().apply {
    shape = GradientDrawable.RECTANGLE
    setStroke(dpToPx(borderWidth).toInt(), borderColor)
    setColor(backgroundColor)
    cornerRadius = this@CapsuleDrawable.cornerRadius * 2
  }

  private val padding: Float = this@CapsuleDrawable.cornerRadius // Padding around the text inside the capsule
  private val textWidth = textPaint.measureText(text)
  private val textHeight = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, 1000)
    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
    .build()
    .height
    .toFloat()
  private val intrinsicWidth = (textWidth + padding * 2).toInt()
  private val intrinsicHeight = textHeight.toInt()
  private val drawLayout = StaticLayout.Builder.obtain(
    text,
    0,
    text.length,
    textPaint,
    intrinsicWidth
  )
    .setAlignment(Layout.Alignment.ALIGN_CENTER)
    .setMaxLines(1)
    .setEllipsize(TextUtils.TruncateAt.END)
    .build()

  override fun draw(canvas: Canvas) {
    // Set the bounds for the capsule drawable
    capsuleDrawable.setBounds(0, 0, intrinsicWidth, intrinsicHeight)

    // Draw the capsule background
    capsuleDrawable.draw(canvas)

    // Positioning for the text
    val x = 0f
    val y = (intrinsicHeight - textHeight) / 2

    // Draw the text centered inside the capsule
    canvas.withTranslation(x, y) {
      drawLayout.draw(this)
    }
  }

  override fun getIntrinsicWidth(): Int {
    return intrinsicWidth
  }

  override fun getIntrinsicHeight(): Int {
    return intrinsicHeight
  }

  override fun setAlpha(alpha: Int) {
    textPaint.alpha = alpha
  }

  override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
    textPaint.colorFilter = colorFilter
  }

  @Deprecated(
    "Not supported",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("android.graphics.PixelFormat.TRANSLUCENT", "android")
  )
  override fun getOpacity(): Int {
    return android.graphics.PixelFormat.TRANSLUCENT
  }

  // Helper method to convert dp to pixels
  private fun dpToPx(dp: Float): Float {
    return dp * context.resources.displayMetrics.density
  }
}
