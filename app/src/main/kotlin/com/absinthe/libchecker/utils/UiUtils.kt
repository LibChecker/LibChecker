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
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withClip
import com.absinthe.libchecker.R
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libraries.utils.utils.UiUtils
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.slider.Slider
import java.io.IOException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import timber.log.Timber

object UiUtils {
  private const val DRAWABLE_STRIP_TILE_PADDING_RATIO = 0.08f

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
    return when (GlobalValues.darkMode) {
      Constants.DARK_MODE_OFF -> AppCompatDelegate.MODE_NIGHT_NO
      Constants.DARK_MODE_ON -> AppCompatDelegate.MODE_NIGHT_YES
      Constants.DARK_MODE_FOLLOW_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
      else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
  }

  fun isSoftInputOpen(): Boolean {
    return SystemServices.inputMethodManager.isActive
  }

  fun createLoadingDialog(context: ContextThemeWrapper): AlertDialog {
    return BaseAlertDialogBuilder(context)
      .setView(
        LinearProgressIndicator(context).apply {
          layoutParams = ViewGroup.LayoutParams(200.dp, ViewGroup.LayoutParams.WRAP_CONTENT).also {
            setPadding(24.dp, 24.dp, 24.dp, 24.dp)
          }
          trackCornerRadius = 3.dp
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

  fun createSnapshotAutoRemoveThresholdDialog(context: ContextThemeWrapper): AlertDialog {
    if (GlobalValues.snapshotAutoRemoveThreshold <= 0) {
      GlobalValues.snapshotAutoRemoveThreshold = 5
    }
    val slider = Slider(context).apply {
      layoutParams =
        ViewGroup.MarginLayoutParams(200.dp, ViewGroup.LayoutParams.WRAP_CONTENT).also {
          setPadding(24.dp, 24.dp, 24.dp, 24.dp)
        }
      stepSize = 1f
      valueFrom = 2f
      valueTo = 10f
      value = GlobalValues.snapshotAutoRemoveThreshold.toFloat()
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
        GlobalValues.snapshotAutoRemoveThreshold = slider.value.toInt()
      }
      .setNegativeButton(android.R.string.cancel, null)
      .create()
  }

  fun addCircleBackground(context: Context, drawable: Drawable, circleColor: Int): Drawable {
    val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 100
    val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 100

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

  @Throws(IOException::class)
  fun getDrawableStrip(
    context: Context,
    drawables: List<Drawable>,
    tileWidthPx: Int? = null,
    tileHeightPx: Int? = null
  ): Bitmap {
    require(drawables.isNotEmpty()) { "Drawables list cannot be empty" }

    val sample = drawables.first()
    val computedWidth = sample.intrinsicWidth.takeIf { it > 0 }
      ?: throw IllegalArgumentException("Drawable must have an intrinsic width")
    val computedHeight = sample.intrinsicHeight.takeIf { it > 0 }
      ?: throw IllegalArgumentException("Drawable must have an intrinsic height")

    val tileWidth = tileWidthPx?.takeIf { it > 0 } ?: computedWidth
    val tileHeight = tileHeightPx?.takeIf { it > 0 } ?: computedHeight

    Timber.d("Tile size: ${tileWidth}x$tileHeight")
    // require(tileWidth == tileHeight) { "Drawable must be square" }

    // drawables.drop(1).forEach { drawable ->
    //   require(drawable.intrinsicWidth == tileWidth && drawable.intrinsicHeight == tileHeight) {
    //     "All drawables must share the same intrinsic size"
    //   }
    // }

    val renderedTiles = drawables.map { renderDrawableToBitmap(it, tileWidth, tileHeight) }
    val contentRects = renderedTiles.map { detectOpaqueBounds(it) }
    val targetEdge = contentRects
      .filterNot { it.isEmpty }
      .maxOfOrNull { max(it.width(), it.height()) }
      ?.coerceAtMost(min(tileWidth, tileHeight))
      ?: min(tileWidth, tileHeight)

    val normalizedTiles = renderedTiles.mapIndexed { index, bitmap ->
      normalizeBitmapToTile(bitmap, contentRects[index], targetEdge, tileWidth, tileHeight)
    }

    val rowCount = min(5, normalizedTiles.size).coerceAtLeast(1)
    val columnCount = ((normalizedTiles.size + rowCount - 1) / rowCount)

    val outputWidth = tileWidth * columnCount
    val outputHeight = tileHeight * rowCount
    val outputBitmap = createBitmap(outputWidth, outputHeight)
    val canvas = Canvas(outputBitmap)

    normalizedTiles.forEachIndexed { index, bitmap ->
      val row = index / columnCount
      val column = index % columnCount
      val left = column * tileWidth
      val top = row * tileHeight
      canvas.drawBitmap(bitmap, left.toFloat(), top.toFloat(), null)
      bitmap.recycle()
    }

    return outputBitmap
  }

  private fun renderDrawableToBitmap(drawable: Drawable, width: Int, height: Int): Bitmap {
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    val boundedDrawable = drawable.constantState?.newDrawable()?.mutate() ?: drawable.mutate()
    boundedDrawable.setBounds(0, 0, width, height)
    boundedDrawable.draw(canvas)
    return bitmap
  }

  private fun detectOpaqueBounds(bitmap: Bitmap): Rect {
    val width = bitmap.width
    val height = bitmap.height
    if (width == 0 || height == 0) {
      return Rect()
    }

    val rect = Rect(width, height, -1, -1)
    val row = IntArray(width)

    for (y in 0 until height) {
      bitmap.getPixels(row, 0, width, 0, y, width, 1)
      for (x in 0 until width) {
        val alpha = row[x] ushr 24
        if (alpha > 0) {
          if (rect.left > x) rect.left = x
          if (rect.top > y) rect.top = y
          if (rect.right < x) rect.right = x
          if (rect.bottom < y) rect.bottom = y
        }
      }
    }

    return if (rect.right >= rect.left && rect.bottom >= rect.top) {
      Rect(rect.left, rect.top, rect.right + 1, rect.bottom + 1)
    } else {
      Rect()
    }
  }

  private fun normalizeBitmapToTile(
    source: Bitmap,
    contentRect: Rect,
    targetEdge: Int,
    tileWidth: Int,
    tileHeight: Int
  ): Bitmap {
    if (contentRect.isEmpty || targetEdge <= 0) {
      return source
    }

    val longestSide = max(contentRect.width(), contentRect.height()).coerceAtLeast(1)
    val minDimension = min(tileWidth, tileHeight)
    val maxPadding = ((minDimension - 1) / 2).coerceAtLeast(0)
    val padding = (minDimension * DRAWABLE_STRIP_TILE_PADDING_RATIO)
      .roundToInt()
      .coerceIn(0, maxPadding)
    val availableWidth = (tileWidth - padding * 2).coerceAtLeast(1)
    val availableHeight = (tileHeight - padding * 2).coerceAtLeast(1)
    val maxAllowedScale = min(
      availableWidth.toFloat() / contentRect.width().coerceAtLeast(1),
      availableHeight.toFloat() / contentRect.height().coerceAtLeast(1)
    )
    val desiredScale = min(targetEdge.toFloat() / longestSide, maxAllowedScale)

    val scaledWidth = (contentRect.width() * desiredScale).roundToInt().coerceAtLeast(1)
    val scaledHeight = (contentRect.height() * desiredScale).roundToInt().coerceAtLeast(1)

    val contentBitmap = Bitmap.createBitmap(
      source,
      contentRect.left,
      contentRect.top,
      contentRect.width(),
      contentRect.height()
    )
    source.recycle()

    val scaledBitmap = if (contentBitmap.width == scaledWidth && contentBitmap.height == scaledHeight) {
      contentBitmap
    } else {
      contentBitmap.scale(scaledWidth, scaledHeight).also {
        contentBitmap.recycle()
      }
    }

    val result = createBitmap(tileWidth, tileHeight)
    val canvas = Canvas(result)
    val horizontalSpace = (availableWidth - scaledWidth).coerceAtLeast(0)
    val verticalSpace = (availableHeight - scaledHeight).coerceAtLeast(0)
    val left = padding + horizontalSpace / 2f
    val top = padding + verticalSpace / 2f
    canvas.drawBitmap(scaledBitmap, left, top, null)

    scaledBitmap.recycle()

    return result
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

  fun Drawable.toCircularBitmap(): Bitmap {
    val bitmap = createBitmap(intrinsicWidth, intrinsicHeight)
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
