package com.absinthe.libchecker.utils

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withClip
import com.absinthe.libchecker.R
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.domain.settings.appearance.NightModeResolver
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libraries.utils.utils.UiUtils
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.slider.Slider
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object UiUtils {
  private val MAX_CIRCLE_BACKGROUND_SIZE = 48.dp

  fun getRandomColor(): Int {
    val range = if (UiUtils.isDarkMode()) {
      (68..136)
    } else {
      (132..200)
    }
    val r = range.random()
    val g = range.random()
    val b = range.random()

    return String.format("#%02x%02x%02x", r, g, b).toColorInt()
  }

  fun isDarkColor(@ColorInt color: Int): Boolean {
    val r = Color.red(color)
    val g = Color.green(color)
    val b = Color.blue(color)
    val brightness = (r * 299 + g * 587 + b * 114) / 1000
    return brightness >= 192
  }

  fun getNightMode(): Int {
    return NightModeResolver.resolve(GlobalValues.darkMode)
  }

  fun isSoftInputOpen(): Boolean {
    return SystemServices.inputMethodManager.isActive
  }

  fun createLoadingDialog(context: ContextThemeWrapper): AlertDialog {
    return BaseAlertDialogBuilder(context)
      .setView(
        LinearProgressIndicator(
          ContextThemeWrapper(context, R.style.App_Widget_M3E_LinearProgressIndicator_Wavy)
        ).apply {
          layoutParams = ViewGroup.LayoutParams(200.dp, ViewGroup.LayoutParams.WRAP_CONTENT).also {
            setPadding(24.dp, 24.dp, 24.dp, 24.dp)
          }
          isIndeterminate = true
        }
      )
      .setCancelable(false)
      .create()
  }

  @Suppress("DEPRECATION")
  fun getScreenAspectRatio(): Float {
    val displayMetrics = DisplayMetrics()
    SystemServices.windowManager.defaultDisplay.getMetrics(displayMetrics)
    val width = displayMetrics.widthPixels
    val height = displayMetrics.heightPixels
    return width.toFloat() / height.toFloat()
  }

  fun hasHinge() = if (OsUtils.atLeastR()) {
    SystemServices.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_HINGE_ANGLE)
  } else {
    false
  }

  fun changeDrawableColor(context: Context, drawableResId: Int, color: Int): Drawable {
    val drawable = ContextCompat.getDrawable(context, drawableResId)?.mutate()
      ?: throw IllegalArgumentException("Drawable is null")
    DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_ATOP)
    DrawableCompat.setTint(drawable, color)
    return drawable
  }

  fun createSnapshotAutoRemoveThresholdDialog(
    context: ContextThemeWrapper,
    initialThreshold: Int,
    onThresholdConfirmed: ((threshold: Int) -> Unit)? = null
  ): AlertDialog {
    val threshold = initialThreshold.takeIf { it > 0 } ?: 5
    val slider = Slider(ContextThemeWrapper(context, R.style.App_Widget_M3E_Slider)).apply {
      layoutParams =
        ViewGroup.MarginLayoutParams(200.dp, ViewGroup.LayoutParams.WRAP_CONTENT).also {
          setPadding(24.dp, 24.dp, 24.dp, 24.dp)
        }
      stepSize = 1f
      valueFrom = 2f
      valueTo = 10f
      value = threshold.toFloat()
    }
    return BaseAlertDialogBuilder(context)
      .setTitle(R.string.album_item_management_snapshot_auto_remove_default_title)
      .setView(slider)
      .setMessage(
        context.getString(
          R.string.album_item_management_snapshot_auto_remove_desc,
          context.getString(android.R.string.ok)
        )
      )
      .setCancelable(false)
      .setPositiveButton(android.R.string.ok) { _, _ ->
        val confirmedThreshold = slider.value.toInt()
        onThresholdConfirmed?.invoke(confirmedThreshold)
      }
      .setNegativeButton(android.R.string.cancel, null)
      .create()
  }

  fun addCircleBackground(context: Context, drawable: Drawable, circleColor: Int): Drawable {
    val intrinsicWidth = drawable.intrinsicWidth.takeIf { it > 0 } ?: 100
    val intrinsicHeight = drawable.intrinsicHeight.takeIf { it > 0 } ?: 100
    val scale = min(1f, MAX_CIRCLE_BACKGROUND_SIZE.toFloat() / max(intrinsicWidth, intrinsicHeight))
    val width = (intrinsicWidth * scale).roundToInt().coerceAtLeast(1)
    val height = (intrinsicHeight * scale).roundToInt().coerceAtLeast(1)

    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = circleColor
    }
    val radius = max(width, height) / 2f
    val cx = width / 2f
    val cy = height / 2f
    canvas.drawCircle(cx, cy, radius, paint)

    val clipPath = Path().apply {
      addCircle(cx, cy, radius, Path.Direction.CW)
    }

    canvas.withClip(clipPath) {
      drawable.setBounds(0, 0, width, height)
      drawable.draw(this)
    }

    return bitmap.toDrawable(context.resources)
  }

  fun drawablesAreEqual(d1: Drawable?, d2: Drawable?): Boolean {
    if (d1 == null || d2 == null) return d1 == d2
    if (d1 === d2) return true

    val bmp1 = d1.toBitmapSafely()
    val bmp2 = d2.toBitmapSafely()

    if (bmp1.width != bmp2.width || bmp1.height != bmp2.height) return false
    return bmp1.sameAs(bmp2)
  }

  fun Drawable.toBitmapSafely(): Bitmap {
    val w = intrinsicWidth.takeIf { it > 0 } ?: 1
    val h = intrinsicHeight.takeIf { it > 0 } ?: 1
    val bitmap = createBitmap(w, h)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, w, h)
    draw(canvas)
    return bitmap
  }

  fun Drawable.toCircularBitmap(maxSize: Int = 128): Bitmap {
    val bitmap = createBitmap(
      intrinsicWidth.coerceAtMost(maxSize),
      intrinsicHeight.coerceAtMost(maxSize)
    )
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)

    val size = min(bitmap.width, bitmap.height)
    val output = createBitmap(size, size)
    val rect = Rect(0, 0, size, size)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val canvasOut = Canvas(output)
    val radius = size / 2f

    canvasOut.drawCircle(radius, radius, radius, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvasOut.drawBitmap(bitmap, null, rect, paint)

    return output
  }
}
