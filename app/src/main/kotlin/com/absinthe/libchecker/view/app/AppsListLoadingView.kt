package com.absinthe.libchecker.view.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.DrawableRes
import androidx.annotation.MainThread
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import com.absinthe.libchecker.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val DEFAULT_DIAMETER_DP = 96
private const val DEFAULT_ROTATION_DURATION = 4000L
private const val DEFAULT_HIGHLIGHT_INTENSITY = 0.35f
private const val DEFAULT_DRAWABLE_SIZE = 256
private const val DEFAULT_MESH_COLUMNS = 48
private const val DEFAULT_MESH_ROWS = 48

/** Custom view that renders a textured sphere with a continuous spin animation. */
@Deprecated("Use RingDotsView instead", ReplaceWith("RingDotsView"))
class AppsListLoadingView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

  private val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
  private val texturePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
  private val meshPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
  private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
  private val shadingPaint = Paint(Paint.ANTI_ALIAS_FLAG)
  private val edgeFadePaint = Paint(Paint.ANTI_ALIAS_FLAG)
  private val shaderMatrix = Matrix()
  private val drawBounds = RectF()
  private val meshColumns = DEFAULT_MESH_COLUMNS
  private val meshRows = DEFAULT_MESH_ROWS

  private var bitmapShader: BitmapShader? = null
  private var lightingShader: Shader? = null
  private var rimShader: Shader? = null
  private var edgeFadeShader: Shader? = null

  private var textureBitmap: Bitmap? = null
  private var frameBitmap: Bitmap? = null
  private var frameCanvas: Canvas? = null
  private var rotationAnimator: ValueAnimator? = null
  private var rotationPhase = 0f
  private var rotationDuration = DEFAULT_ROTATION_DURATION
  private var highlightStrength = DEFAULT_HIGHLIGHT_INTENSITY
  private var meshVertices = FloatArray(0)
  private var baseTheta = FloatArray(0)
  private var basePhi = FloatArray(0)
  private var axisTiltRadians = 0f
  private val defaultDiameterPx: Int

  private var isStart = false

  init {
    setLayerType(LAYER_TYPE_HARDWARE, null)
    fallbackPaint.style = Paint.Style.FILL
    fallbackPaint.color = ColorUtils.setAlphaComponent(Color.LTGRAY, 220)
    texturePaint.style = Paint.Style.FILL
    texturePaint.isFilterBitmap = true
    meshPaint.style = Paint.Style.FILL
    meshPaint.isFilterBitmap = true
    meshPaint.isDither = true
    highlightPaint.style = Paint.Style.FILL
    shadingPaint.style = Paint.Style.FILL
    edgeFadePaint.style = Paint.Style.FILL
    edgeFadePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)

    defaultDiameterPx = (DEFAULT_DIAMETER_DP * resources.displayMetrics.density).roundToInt()

    context.obtainStyledAttributes(attrs, R.styleable.AppsListLoadingView, defStyleAttr, 0)
      .use { typedArray ->
        val duration = typedArray.getInt(
          R.styleable.AppsListLoadingView_alvRotationDuration,
          DEFAULT_ROTATION_DURATION.toInt()
        ).toLong()
        rotationDuration = duration.coerceAtLeast(500L)

        highlightStrength = typedArray
          .getFloat(
            R.styleable.AppsListLoadingView_alvHighlightStrength,
            DEFAULT_HIGHLIGHT_INTENSITY
          )
          .coerceIn(0f, 1f)

        val tiltDegrees = typedArray.getFloat(R.styleable.AppsListLoadingView_alvAxisTilt, 0f)
        if (tiltDegrees != 0f) {
          setAxisTiltDegrees(tiltDegrees)
        }

        val drawableId = typedArray.getResourceId(R.styleable.AppsListLoadingView_alvSurfaceSrc, 0)
        if (drawableId != 0) {
          ContextCompat.getDrawable(context, drawableId)?.let { setSurfaceDrawable(it) }
        }
      }

    updateHighlightShaders()
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val desiredWidth = defaultDiameterPx + paddingLeft + paddingRight
    val desiredHeight = defaultDiameterPx + paddingTop + paddingBottom
    val measuredWidth = resolveSize(desiredWidth, widthMeasureSpec)
    val measuredHeight = resolveSize(desiredHeight, heightMeasureSpec)
    setMeasuredDimension(measuredWidth, measuredHeight)
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    updateDrawBounds(w, h)
    rebuildSphereShader()
    updateHighlightShaders()
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    if (textureBitmap != null && isStart) {
      startSpinning()
    }
  }

  override fun onDetachedFromWindow() {
    stopSpinning()
    super.onDetachedFromWindow()
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    if (drawBounds.isEmpty) {
      return
    }

    val radius = drawBounds.width() / 2f
    val cx = drawBounds.centerX()
    val cy = drawBounds.centerY()

    val layerId = canvas.saveLayer(drawBounds, null)

    val frame = frameBitmap
    val hasTexture = textureBitmap != null && frame != null && meshVertices.isNotEmpty()
    if (hasTexture) {
      renderTextureFrame()
      canvas.drawBitmapMesh(
        frame,
        meshColumns,
        meshRows,
        meshVertices,
        0,
        null,
        0,
        meshPaint
      )
    } else {
      canvas.drawCircle(cx, cy, radius, fallbackPaint)
    }

    rimShader?.let {
      shadingPaint.shader = it
      canvas.drawCircle(cx, cy, radius, shadingPaint)
    }
    lightingShader?.let {
      highlightPaint.shader = it
      if (highlightStrength > 0f) {
        canvas.drawCircle(cx, cy, radius, highlightPaint)
      }
    }

    edgeFadeShader?.let {
      edgeFadePaint.shader = it
      canvas.drawCircle(cx, cy, radius, edgeFadePaint)
    }

    canvas.restoreToCount(layerId)
  }

  @MainThread
  fun setSurfaceDrawable(drawable: Drawable?) {
    if (drawable == null) {
      updateTextureBitmap(null)
      return
    }
    updateTextureBitmap(convertDrawableToBitmap(drawable))
  }

  @MainThread
  fun setSurfaceResource(@DrawableRes resId: Int) {
    if (resId == 0) {
      updateTextureBitmap(null)
      return
    }
    ContextCompat.getDrawable(context, resId)?.let { setSurfaceDrawable(it) }
  }

  @MainThread
  fun setSurfaceBitmap(bitmap: Bitmap?) {
    if (bitmap == null) {
      updateTextureBitmap(null)
      return
    }
    require(!bitmap.isRecycled) { "Bitmap has already been recycled" }
    val safeBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
    updateTextureBitmap(safeBitmap)
  }

  @MainThread
  fun setRotationDuration(durationMs: Long) {
    if (rotationDuration == durationMs) {
      return
    }
    rotationDuration = durationMs.coerceAtLeast(500L)
    rotationAnimator?.duration = rotationDuration
  }

  @MainThread
  fun setHighlightStrength(strength: Float) {
    val clamped = strength.coerceIn(0f, 1f)
    if (highlightStrength == clamped) {
      return
    }
    highlightStrength = clamped
    updateHighlightShaders()
    invalidate()
  }

  @MainThread
  fun setAxisTiltDegrees(degrees: Float) {
    val clamped = degrees.coerceIn(-89f, 89f)
    val radians = clamped * (PI.toFloat() / 180f)
    if (axisTiltRadians == radians) {
      return
    }
    axisTiltRadians = radians
    updateMeshVertices()
    updateHighlightShaders()
    invalidate()
  }

  fun startSpinning() {
    isStart = true
    if (textureBitmap == null) {
      return
    }
    ensureAnimator()
    if (rotationAnimator?.isStarted != true) {
      rotationAnimator?.start()
    }
  }

  fun stopSpinning() {
    isStart = false
    rotationAnimator?.cancel()
  }

  fun setPreloadedBitmap(type: TextureType) {
    val dir = when (type) {
      TextureType.APPS -> "apps"
      TextureType.LIBS -> "libs"
    }
    val baseDir = context.filesDir.resolve("sphere_texture").resolve(dir)
    if (baseDir.exists()) {
      val textures = baseDir.listFiles()?.filter { it.extension == "png" }.orEmpty()

      if (textures.isNotEmpty()) {
        val bitmap = BitmapFactory.decodeFile(textures.random().path)
        post { setSurfaceBitmap(bitmap) }
      }
    }
  }

  private fun ensureAnimator() {
    if (rotationAnimator == null) {
      rotationAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = rotationDuration
        interpolator = LinearInterpolator()
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener { animator ->
          val value = animator.animatedValue as Float
          rotationPhase = value
          applyRotationMatrix()
          invalidate()
        }
      }
    }
  }

  private fun updateTextureBitmap(newBitmap: Bitmap?) {
    if (textureBitmap === newBitmap) {
      return
    }
    textureBitmap?.recycle()
    textureBitmap = newBitmap
    if (newBitmap != null) {
      ensureFrameBuffer(newBitmap)
    } else {
      releaseFrameBuffer()
    }
    rebuildSphereShader()
    if (isStart && isAttachedToWindow && textureBitmap != null) {
      startSpinning()
    } else if (textureBitmap == null) {
      stopSpinning()
    }
    invalidate()
  }

  private fun rebuildSphereShader() {
    val bitmap = textureBitmap ?: run {
      bitmapShader = null
      texturePaint.shader = null
      return
    }
    ensureFrameBuffer(bitmap)
    if (drawBounds.isEmpty) {
      return
    }

    val shader = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.CLAMP)
    bitmapShader = shader
    texturePaint.shader = shader
    applyRotationMatrix()
  }

  private fun applyRotationMatrix() {
    val bitmap = textureBitmap ?: return
    val shader = bitmapShader ?: return
    if (drawBounds.isEmpty) {
      return
    }

    shaderMatrix.reset()
    val phase = (rotationPhase % 1f + 1f) % 1f
    val shift = phase * bitmap.width
    shaderMatrix.postTranslate(-shift, 0f)

    shader.setLocalMatrix(shaderMatrix)
  }

  private fun renderTextureFrame() {
    val shader = bitmapShader ?: return
    val frame = frameBitmap ?: return
    val canvas = frameCanvas ?: return
    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    texturePaint.shader = shader
    canvas.drawRect(0f, 0f, frame.width.toFloat(), frame.height.toFloat(), texturePaint)
  }

  private fun ensureFrameBuffer(source: Bitmap) {
    val current = frameBitmap
    if (current != null && current.width == source.width && current.height == source.height) {
      return
    }
    releaseFrameBuffer()
    frameBitmap = createBitmap(source.width, source.height)
    frameCanvas = Canvas(frameBitmap!!)
  }

  private fun releaseFrameBuffer() {
    frameCanvas = null
    frameBitmap?.recycle()
    frameBitmap = null
  }

  private fun ensureMeshBaseCoordinates() {
    val vertexCount = (meshColumns + 1) * (meshRows + 1)
    if (baseTheta.size == vertexCount && basePhi.size == vertexCount) {
      return
    }
    baseTheta = FloatArray(vertexCount)
    basePhi = FloatArray(vertexCount)
    var index = 0
    for (row in 0..meshRows) {
      val v = row.toFloat() / meshRows.toFloat()
      val phi = ((0.5f - v) * PI).toFloat()
      for (col in 0..meshColumns) {
        val u = col.toFloat() / meshColumns.toFloat()
        val theta = ((u - 0.5f) * PI).toFloat()
        basePhi[index] = phi
        baseTheta[index] = theta
        index++
      }
    }
  }

  private fun updateMeshVertices() {
    if (drawBounds.isEmpty) {
      meshVertices = FloatArray(0)
      return
    }

    ensureMeshBaseCoordinates()
    val vertexCount = baseTheta.size
    if (meshVertices.size != vertexCount * 2) {
      meshVertices = FloatArray(vertexCount * 2)
    }

    val radius = drawBounds.width() / 2f
    if (radius <= 0f) {
      meshVertices = FloatArray(0)
      return
    }

    val cx = drawBounds.centerX()
    val cy = drawBounds.centerY()
    val cosTilt = cos(axisTiltRadians.toDouble())
    val sinTilt = sin(axisTiltRadians.toDouble())

    var writeIndex = 0
    for (i in 0 until vertexCount) {
      val phi = basePhi[i].toDouble()
      val theta = baseTheta[i].toDouble()

      val cosPhi = cos(phi)
      val sinPhi = sin(phi)
      val cosTheta = cos(theta)
      val sinTheta = sin(theta)

      val x = (cosPhi * sinTheta).toFloat()
      val y = sinPhi.toFloat()
      val z = (cosPhi * cosTheta).toFloat()

      val yTilt = (y * cosTilt - z * sinTilt).toFloat()

      meshVertices[writeIndex++] = cx + radius * x
      meshVertices[writeIndex++] = cy - radius * yTilt
    }
  }

  private fun updateDrawBounds(width: Int, height: Int) {
    val contentWidth = width - paddingLeft - paddingRight
    val contentHeight = height - paddingTop - paddingBottom
    val diameter = min(contentWidth, contentHeight).toFloat()
    if (diameter <= 0f) {
      drawBounds.setEmpty()
      meshVertices = FloatArray(0)
      return
    }

    val left = paddingLeft + (contentWidth - diameter) / 2f
    val top = paddingTop + (contentHeight - diameter) / 2f
    drawBounds.set(left, top, left + diameter, top + diameter)
    updateMeshVertices()
  }

  private fun updateHighlightShaders() {
    if (drawBounds.isEmpty) {
      lightingShader = null
      rimShader = null
      edgeFadeShader = null
      return
    }

    val radius = drawBounds.width() / 2f
    val cx = drawBounds.centerX()
    val cy = drawBounds.centerY()
    val tiltSin = sin(axisTiltRadians.toDouble()).toFloat()
    val highlightYOffset = radius * 0.4f * tiltSin
    val rimYOffset = radius * 0.15f * tiltSin

    lightingShader = if (highlightStrength > 0f) {
      val alpha = (highlightStrength * 255f).roundToInt().coerceIn(0, 255)
      val highlightColor = ColorUtils.setAlphaComponent(Color.WHITE, alpha)
      android.graphics.RadialGradient(
        cx - radius * 0.35f,
        cy - radius * 0.45f + highlightYOffset,
        radius,
        intArrayOf(highlightColor, ColorUtils.setAlphaComponent(highlightColor, 0)),
        floatArrayOf(0f, 1f),
        Shader.TileMode.CLAMP
      )
    } else {
      null
    }

    val shadowAlphaStart = (0.25f * 255f).roundToInt().coerceIn(0, 255)
    val shadowAlphaEnd = (0.45f * 255f).roundToInt().coerceIn(0, 255)
    rimShader = LinearGradient(
      drawBounds.left,
      cy + rimYOffset,
      drawBounds.right,
      cy + rimYOffset,
      intArrayOf(
        ColorUtils.setAlphaComponent(Color.BLACK, shadowAlphaStart),
        Color.TRANSPARENT,
        ColorUtils.setAlphaComponent(Color.BLACK, shadowAlphaEnd)
      ),
      floatArrayOf(0f, 0.45f, 1f),
      Shader.TileMode.CLAMP
    )

    updateEdgeFadeShader()
  }

  private fun updateEdgeFadeShader() {
    if (drawBounds.isEmpty) {
      edgeFadeShader = null
      return
    }

    val radius = drawBounds.width() / 2f
    val cx = drawBounds.centerX()
    val cy = drawBounds.centerY()
    val fadeRatio = 0.12f
    edgeFadeShader = android.graphics.RadialGradient(
      cx,
      cy,
      radius,
      intArrayOf(
        Color.TRANSPARENT,
        Color.TRANSPARENT,
        ColorUtils.setAlphaComponent(Color.BLACK, 255)
      ),
      floatArrayOf(0f, 1f - fadeRatio, 1f),
      Shader.TileMode.CLAMP
    )
  }

  private fun convertDrawableToBitmap(drawable: Drawable): Bitmap {
    val workingDrawable = when (drawable) {
      is BitmapDrawable -> drawable
      else -> drawable.constantState?.newDrawable()?.mutate() ?: drawable.mutate()
    }

    if (workingDrawable is BitmapDrawable) {
      val bitmap = workingDrawable.bitmap
      if (bitmap != null && !bitmap.isRecycled) {
        return bitmap.copy(Bitmap.Config.ARGB_8888, false)
      }
    }

    val intrinsicWidth = workingDrawable.intrinsicWidth.takeIf { it > 0 } ?: DEFAULT_DRAWABLE_SIZE
    val intrinsicHeight = workingDrawable.intrinsicHeight.takeIf { it > 0 } ?: DEFAULT_DRAWABLE_SIZE
    val size = max(intrinsicWidth, intrinsicHeight)
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)
    val left = (size - intrinsicWidth) / 2
    val top = (size - intrinsicHeight) / 2
    val oldBounds = workingDrawable.copyBounds()
    workingDrawable.setBounds(left, top, left + intrinsicWidth, top + intrinsicHeight)
    workingDrawable.draw(canvas)
    if (!oldBounds.isEmpty) {
      workingDrawable.setBounds(oldBounds.left, oldBounds.top, oldBounds.right, oldBounds.bottom)
    }
    return bitmap
  }

  enum class TextureType {
    APPS,
    LIBS
  }
}
