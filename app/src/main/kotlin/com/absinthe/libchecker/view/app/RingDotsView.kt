package com.absinthe.libchecker.view.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.dpToDimension
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.rulesbundle.IconResMap
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

class RingDotsView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

  companion object {
    private const val DOT_COUNT = 24
    private const val ROTATE_DURATION = 12000L
    private const val HIGHLIGHT_DURATION = 850L
    private const val HIGHLIGHT_HOLD_DURATION = 1250L
    private const val HIGHLIGHT_RING_PUSH = 0.09f
    private const val HIGHLIGHT_INNER_RING_PUSH = 0.14f
    private const val HIGHLIGHT_MAX_SCALE = 3.4f
    private const val HIGHLIGHT_HALO_SCALE = 1.12f
    private const val MAX_HIGHLIGHT_BITMAP_BYTES = 150 * 1024 * 1024
    private const val RING_ALPHA = 232
    private const val RULE_ICON_POOL_COUNT = 100
    private const val HIGHLIGHT_PROVIDER_MAX_MISSES = 8
    private const val HIGHLIGHT_PROVIDER_RETRY_DELAY_MS = 120L
  }

  private data class RingSpec(
    val radiusFraction: Float,
    val dotRadiusPx: Float,
    val rotationScale: Float,
    val colorShift: Float
  )

  private val ringSpecs = listOf(
    RingSpec(0.58f, context.dpToDimension(2.2f), 0.86f, -0.02f),
    RingSpec(0.70f, context.dpToDimension(3.5f), 0.96f, 0.04f),
    RingSpec(0.82f, context.dpToDimension(3.1f), 1.05f, 0.08f),
    RingSpec(0.9f, context.dpToDimension(5f), 1.14f, 0.12f)
  )
  private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.FILL
  }
  private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    isFilterBitmap = true
  }
  private val highlightDst = RectF()
  private val highlightSurfaceColor = context.getColorByAttr(com.google.android.material.R.attr.colorSurface)
  private val gradientStops = intArrayOf(
    0xFF64D2FF.toInt(),
    0xFF0A84FF.toInt(),
    0xFFBF5AF2.toInt(),
    0xFFFF375F.toInt(),
    0xFFFF9F0A.toInt(),
    0xFF30D158.toInt()
  )
  private val gradientPositions = floatArrayOf(0f, 0.18f, 0.39f, 0.62f, 0.82f, 1f)
  private val dotColors = Array(ringSpecs.size) { ringIndex ->
    IntArray(DOT_COUNT) { dotIndex ->
      sampleGradient(normalizeUnit(dotIndex * (360f / DOT_COUNT) / 360f + ringSpecs[ringIndex].colorShift))
    }
  }
  private val highlightGapPx = context.dpToDimension(3f)
  private val ringRotations = FloatArray(ringSpecs.size)
  private val phaseOffsets = computePhaseOffsets()
  private var ringRadii = FloatArray(ringSpecs.size)

  private var isRunning = false
  private var frameAnimator: ValueAnimator? = null
  private var lastAnimatorValue = 0f

  private var highlightLoader: (suspend () -> Bitmap?)? = null
  private var iconJob: Job? = null
  private var providerGeneration = 0
  private var currentHighlightBitmap: Bitmap? = null
  private var pendingHighlightBitmap: Bitmap? = null
  private var highlightIndex = -1
  private var highlightStartedAt = 0L
  private var highlightProgress = 0f
  private var outgoingHighlightBitmap: Bitmap? = null
  private var outgoingHighlightIndex = -1
  private var outgoingHighlightStartedAt = 0L
  private var outgoingHighlightProgress = 0f

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    updateRingRadii()
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    if (isRunning) {
      startAnimator()
      requestHighlight()
    }
  }

  override fun onDetachedFromWindow() {
    stopAnimator()
    releaseHighlights()
    resetRotations()
    super.onDetachedFromWindow()
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    if (width <= 0 || height <= 0) return

    if (ringRadii.size != ringSpecs.size) {
      updateRingRadii()
    }
    val cx = width / 2f
    val cy = height / 2f
    for (ringIndex in ringSpecs.indices) {
      drawRing(canvas, cx, cy, ringIndex, ringSpecs[ringIndex])
    }
    drawHighlight(canvas, cx, cy, outgoingHighlightBitmap, outgoingHighlightIndex, outgoingHighlightProgress)
    drawHighlight(canvas, cx, cy)
  }

  fun setAppIconHighlightProvider(loadIcon: suspend () -> Drawable?) {
    setHighlightLoader { loadAppHighlightBitmap(loadIcon) }
  }

  fun setRuleIconHighlightProvider(
    withCircleBackground: Boolean = false,
    @ColorRes circleBackgroundColorRes: Int = R.color.feature_background
  ) {
    setHighlightLoader {
      loadRuleHighlightBitmap(withCircleBackground, circleBackgroundColorRes)
    }
  }

  fun start() {
    if (isRunning) return
    isRunning = true
    if (isAttachedToWindow) {
      startAnimator()
      requestHighlight()
    }
  }

  fun stop() {
    if (!isRunning && frameAnimator == null && iconJob == null) return
    isRunning = false
    stopAnimator()
    releaseHighlights()
    resetRotations()
    invalidateOnAnimationFrame()
  }

  private fun startAnimator() {
    if (frameAnimator != null || !isRunning || !isAttachedToWindow) return

    lastAnimatorValue = 0f
    frameAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
      duration = ROTATE_DURATION
      interpolator = LinearInterpolator()
      repeatCount = ValueAnimator.INFINITE
      addUpdateListener { animator ->
        val value = animator.animatedValue as Float
        var delta = value - lastAnimatorValue
        if (delta < 0f) {
          delta += 360f
        }
        if (delta != 0f) {
          for (index in ringSpecs.indices) {
            ringRotations[index] = normalizeDegrees(ringRotations[index] + delta * ringSpecs[index].rotationScale)
          }
        }
        lastAnimatorValue = value
        updateHighlight(SystemClock.uptimeMillis())
        invalidateOnAnimationFrame()
      }
      start()
    }
  }

  private fun stopAnimator() {
    frameAnimator?.apply {
      removeAllUpdateListeners()
      cancel()
    }
    frameAnimator = null
    lastAnimatorValue = 0f
  }

  private fun updateHighlight(now: Long) {
    if (outgoingHighlightBitmap != null) {
      outgoingHighlightProgress = computeHighlightProgress((now - outgoingHighlightStartedAt).coerceAtLeast(0L))
      if (outgoingHighlightProgress <= 0f) clearOutgoingHighlight()
    }
    if (currentHighlightBitmap == null) {
      pendingHighlightBitmap?.let { bitmap ->
        pendingHighlightBitmap = null
        activateHighlight(bitmap, now)
      }
      requestHighlight()
      return
    }

    val elapsed = (now - highlightStartedAt).coerceAtLeast(0L)
    val cycleDuration = HIGHLIGHT_DURATION + HIGHLIGHT_HOLD_DURATION
    val shrinkStartedAt = HIGHLIGHT_DURATION * 0.3f + HIGHLIGHT_HOLD_DURATION
    if (elapsed >= shrinkStartedAt && pendingHighlightBitmap != null) {
      clearOutgoingHighlight()
      outgoingHighlightBitmap = currentHighlightBitmap
      outgoingHighlightIndex = highlightIndex
      outgoingHighlightStartedAt = highlightStartedAt
      outgoingHighlightProgress = computeHighlightProgress(elapsed)
      val next = checkNotNull(pendingHighlightBitmap)
      pendingHighlightBitmap = null
      activateHighlight(next, now)
      requestHighlight()
    } else if (elapsed >= cycleDuration) {
      clearCurrentHighlight()
      pendingHighlightBitmap?.let { bitmap ->
        pendingHighlightBitmap = null
        activateHighlight(bitmap, now)
      }
      requestHighlight()
    } else {
      highlightProgress = computeHighlightProgress(elapsed)
    }
  }

  private fun setHighlightLoader(loader: suspend () -> Bitmap?) {
    providerGeneration++
    cancelIconLoad()
    clearHighlightBitmaps()
    highlightLoader = loader
    if (isRunning && isAttachedToWindow) {
      requestHighlight()
    } else {
      invalidateOnAnimationFrame()
    }
  }

  private fun requestHighlight() {
    val loader = highlightLoader ?: return
    if (!isRunning || !isAttachedToWindow || pendingHighlightBitmap != null || iconJob?.isActive == true) {
      return
    }

    val generation = providerGeneration
    lateinit var job: Job
    job = CoroutineScope(Dispatchers.IO).launch {
      val bitmap = try {
        loader()
      } catch (_: CancellationException) {
        return@launch
      } catch (_: Exception) {
        delay(HIGHLIGHT_PROVIDER_RETRY_DELAY_MS)
        null
      }
      val accepted = bitmap?.takeUnless { it.isRecycled || it.byteCount > MAX_HIGHLIGHT_BITMAP_BYTES }
      if (accepted !== bitmap) {
        bitmap?.let(::recycleBitmap)
      }
      val posted = post {
        if (iconJob === job) {
          iconJob = null
        }
        if (accepted == null) {
          if (generation == providerGeneration && isRunning && isAttachedToWindow) {
            requestHighlight()
          }
          return@post
        }
        if (generation != providerGeneration || !isRunning || !isAttachedToWindow) {
          recycleBitmap(accepted)
          return@post
        }
        if (currentHighlightBitmap == null) {
          activateHighlight(accepted, SystemClock.uptimeMillis())
          requestHighlight()
        } else {
          pendingHighlightBitmap?.let(::recycleBitmap)
          pendingHighlightBitmap = accepted
        }
        invalidateOnAnimationFrame()
      }
      if (!posted) {
        accepted?.let(::recycleBitmap)
      }
    }
    iconJob = job
  }

  private fun activateHighlight(bitmap: Bitmap, now: Long) {
    currentHighlightBitmap = bitmap
    highlightIndex = pickNextHighlight(highlightIndex)
    highlightStartedAt = now
    highlightProgress = 0f
  }

  private fun releaseHighlights() {
    providerGeneration++
    cancelIconLoad()
    clearHighlightBitmaps()
  }

  private fun cancelIconLoad() {
    iconJob?.cancel()
    iconJob = null
  }

  private fun clearHighlightBitmaps() {
    clearCurrentHighlight()
    clearOutgoingHighlight()
    pendingHighlightBitmap?.let(::recycleBitmap)
    pendingHighlightBitmap = null
  }

  private fun clearOutgoingHighlight() {
    outgoingHighlightBitmap?.let(::recycleBitmap)
    outgoingHighlightBitmap = null
    outgoingHighlightIndex = -1
    outgoingHighlightProgress = 0f
  }

  private fun clearCurrentHighlight() {
    currentHighlightBitmap?.let(::recycleBitmap)
    currentHighlightBitmap = null
    highlightProgress = 0f
  }

  private suspend fun loadAppHighlightBitmap(loadIcon: suspend () -> Drawable?): Bitmap? {
    val defaultIcon = context.packageManager.defaultActivityIcon
    var misses = 0
    while (true) {
      currentCoroutineContext().ensureActive()
      val icon = loadIcon()
      if (icon == null) {
        delay(HIGHLIGHT_PROVIDER_RETRY_DELAY_MS)
        continue
      }
      if (UiUtils.drawablesAreEqual(icon, defaultIcon)) {
        misses = delayAfterMisses(misses)
        continue
      }
      return icon.toHighlightBitmap()
    }
  }

  private suspend fun loadRuleHighlightBitmap(
    withCircleBackground: Boolean,
    @ColorRes circleBackgroundColorRes: Int
  ): Bitmap? {
    var misses = 0
    while (true) {
      currentCoroutineContext().ensureActive()
      val index = Random.nextInt(RULE_ICON_POOL_COUNT)
      val drawable = if (IconResMap.isSingleColorIcon(index)) {
        null
      } else {
        ContextCompat.getDrawable(context, IconResMap.getIconRes(index))
      }
      if (drawable == null) {
        misses = delayAfterMisses(misses)
        continue
      }
      val icon = if (withCircleBackground) {
        UiUtils.addCircleBackground(
          context,
          drawable,
          ContextCompat.getColor(context, circleBackgroundColorRes)
        )
      } else {
        drawable
      }
      return icon.toHighlightBitmap()
    }
  }

  private fun Drawable.toHighlightBitmap(): Bitmap {
    val width = intrinsicWidth.coerceAtLeast(1)
    val height = intrinsicHeight.coerceAtLeast(1)
    val scale = ringSpecs.last().dotRadiusPx * HIGHLIGHT_MAX_SCALE * 2f / max(width, height)
    // Own the bitmap so releasing a highlight cannot recycle a provider's cached icon.
    return toBitmap(
      (width * scale).roundToInt().coerceAtLeast(1),
      (height * scale).roundToInt().coerceAtLeast(1)
    ).copy(Bitmap.Config.ARGB_8888, false)
  }

  private suspend fun delayAfterMisses(previousMisses: Int): Int {
    val misses = previousMisses + 1
    if (misses < HIGHLIGHT_PROVIDER_MAX_MISSES) return misses
    delay(HIGHLIGHT_PROVIDER_RETRY_DELAY_MS)
    return 0
  }

  private fun drawRing(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    ringIndex: Int,
    spec: RingSpec
  ) {
    val angleStep = 360f / DOT_COUNT
    val colors = dotColors[ringIndex]
    val baseRadius = ringRadii.getOrElse(ringIndex) { 0f }
    val phaseOffset = phaseOffsets.getOrElse(ringIndex) { 0f }
    val rotation = ringRotations.getOrElse(ringIndex) { 0f }
    for (dotIndex in 0 until DOT_COUNT) {
      val baseAngle = dotIndex * angleStep
      val angle = normalizeDegrees(baseAngle + phaseOffset + rotation)
      val dotProgress = when (dotIndex) {
        highlightIndex -> highlightProgress
        outgoingHighlightIndex -> outgoingHighlightProgress
        else -> 0f
      }
      val isHighlightDot = ringIndex == ringSpecs.lastIndex && dotProgress > 0f
      var pressure = 0f
      for (slot in 0..1) {
        val index = if (slot == 0) highlightIndex else outgoingHighlightIndex
        val progress = if (slot == 0) highlightProgress else outgoingHighlightProgress
        val highlightAngle = resolveHighlightAngle(index) ?: continue
        val distance = abs((angle - highlightAngle + 540f) % 360f - 180f)
        val influence = (1f - distance / (angleStep * 2.5f)).coerceIn(0f, 1f)
        pressure = max(pressure, influence * influence * (3f - 2f * influence) * progress)
      }
      val radius = when {
        isHighlightDot -> baseRadius * (1f + HIGHLIGHT_RING_PUSH * dotProgress)
        ringIndex < ringSpecs.lastIndex -> baseRadius * (1f - HIGHLIGHT_INNER_RING_PUSH * pressure)
        else -> baseRadius
      }
      val angleRad = Math.toRadians(angle.toDouble())
      var x = cx + (cos(angleRad) * radius).toFloat()
      var y = cy + (sin(angleRad) * radius).toFloat()
      if (ringIndex == ringSpecs.lastIndex && !isHighlightDot) {
        for (slot in 0..1) {
          val index = if (slot == 0) highlightIndex else outgoingHighlightIndex
          val progress = if (slot == 0) highlightProgress else outgoingHighlightProgress
          val highlightAngle = resolveHighlightAngle(index) ?: continue
          if (progress <= 0f) continue
          val orbit = baseRadius * (1f + HIGHLIGHT_RING_PUSH * progress)
          val radians = Math.toRadians(highlightAngle.toDouble())
          val dx = x - cx - (cos(radians) * orbit).toFloat()
          val dy = y - cy - (sin(radians) * orbit).toFloat()
          val clearance = spec.dotRadiusPx * (2f + progress * (HIGHLIGHT_MAX_SCALE - 1f)) + highlightGapPx * progress
          if (dx * dx + dy * dy < clearance * clearance) {
            val distance = hypot(dx, dy).coerceAtLeast(0.001f)
            val displacement = (clearance - distance).coerceAtLeast(0f) / distance
            x += dx * displacement
            y += dy * displacement
          }
        }
      }
      val alpha = if (isHighlightDot) {
        (RING_ALPHA * (1f - dotProgress)).roundToInt()
      } else {
        RING_ALPHA
      }
      paint.color = ColorUtils.setAlphaComponent(colors[dotIndex], alpha)
      canvas.drawCircle(x, y, spec.dotRadiusPx, paint)
    }
    paint.alpha = 255
  }

  private fun drawHighlight(canvas: Canvas, cx: Float, cy: Float) {
    drawHighlight(canvas, cx, cy, currentHighlightBitmap, highlightIndex, highlightProgress)
  }

  private fun drawHighlight(canvas: Canvas, cx: Float, cy: Float, icon: Bitmap?, highlightIndex: Int, highlightProgress: Float) {
    val bitmap = icon?.takeUnless { it.isRecycled } ?: return
    if (highlightIndex < 0 || highlightProgress <= 0f) return

    val ringIndex = ringSpecs.lastIndex
    val spec = ringSpecs[ringIndex]
    val baseAngle = highlightIndex * (360f / DOT_COUNT)
    val angle = normalizeDegrees(baseAngle + phaseOffsets[ringIndex] + ringRotations[ringIndex])
    val radius = ringRadii[ringIndex] * (1f + HIGHLIGHT_RING_PUSH * highlightProgress)
    val angleRad = Math.toRadians(angle.toDouble())
    val x = cx + (cos(angleRad) * radius).toFloat()
    val y = cy + (sin(angleRad) * radius).toFloat()
    val size = spec.dotRadiusPx * (1f + highlightProgress * (HIGHLIGHT_MAX_SCALE - 1f))
    val tint = dotColors[ringIndex][highlightIndex]
    val alpha = (255 * highlightProgress).toInt().coerceIn(0, 255)

    paint.color = ColorUtils.setAlphaComponent(tint, (alpha * 0.12f).toInt())
    canvas.drawCircle(x, y, size * HIGHLIGHT_HALO_SCALE, paint)
    paint.color = ColorUtils.setAlphaComponent(ColorUtils.blendARGB(highlightSurfaceColor, tint, 0.06f), alpha)
    canvas.drawCircle(x, y, size, paint)

    val iconScale = size * 0.86f / max(bitmap.width, bitmap.height)
    val halfWidth = bitmap.width * iconScale
    val halfHeight = bitmap.height * iconScale
    highlightDst.set(x - halfWidth, y - halfHeight, x + halfWidth, y + halfHeight)
    bitmapPaint.alpha = alpha
    canvas.drawBitmap(bitmap, null, highlightDst, bitmapPaint)
    bitmapPaint.alpha = 255
  }

  private fun updateRingRadii() {
    val largestDot = ringSpecs.maxOf { it.dotRadiusPx }
    val highlightOutset = largestDot * HIGHLIGHT_MAX_SCALE * HIGHLIGHT_HALO_SCALE
    val outerRadius = (
      (min(width, height) / 2f - highlightOutset - context.dpToDimension(1f)) /
        (1f + HIGHLIGHT_RING_PUSH)
      ).coerceAtLeast(0f)
    var nextRadius = outerRadius
    ringRadii = FloatArray(ringSpecs.size)
    for (index in ringSpecs.lastIndex downTo 0) {
      val spec = ringSpecs[index]
      val radius = if (index == ringSpecs.lastIndex) {
        outerRadius
      } else {
        min(outerRadius * spec.radiusFraction, max(0f, nextRadius - spec.dotRadiusPx * 1.05f))
      }
      ringRadii[index] = radius.coerceAtLeast(0f)
      nextRadius = ringRadii[index]
    }
  }

  private fun computePhaseOffsets(): FloatArray {
    val angleStep = 360f / DOT_COUNT
    return FloatArray(ringSpecs.size) { index ->
      normalizeDegrees((ringSpecs.lastIndex - index) * angleStep / 2f)
    }
  }

  private fun computeHighlightProgress(elapsed: Long): Float {
    val growDuration = HIGHLIGHT_DURATION * 0.3f
    val shrinkDuration = HIGHLIGHT_DURATION - growDuration
    return when {
      elapsed <= growDuration -> easeOutCubic((elapsed / growDuration).coerceIn(0f, 1f))

      elapsed <= growDuration + HIGHLIGHT_HOLD_DURATION -> 1f

      else -> {
        val progress = ((elapsed - growDuration - HIGHLIGHT_HOLD_DURATION) / shrinkDuration)
          .coerceIn(0f, 1f)
        (1f - easeOutCubic(progress)).coerceIn(0f, 1f)
      }
    }
  }

  private fun pickNextHighlight(previous: Int): Int {
    if (previous < 0) return Random.nextInt(DOT_COUNT)
    // Keep the incoming and outgoing icons apart during their overlap.
    return (previous + Random.nextInt(DOT_COUNT / 4, DOT_COUNT * 3 / 4 + 1)) % DOT_COUNT
  }

  private fun sampleGradient(position: Float): Int {
    val p = normalizeUnit(position)
    for (index in 0 until gradientStops.lastIndex) {
      val start = gradientPositions[index]
      val end = gradientPositions[index + 1]
      if (p <= end) {
        val fraction = ((p - start) / max(end - start, 0.0001f)).coerceIn(0f, 1f)
        return ColorUtils.blendARGB(gradientStops[index], gradientStops[index + 1], fraction)
      }
    }
    return gradientStops.last()
  }

  private fun resolveHighlightAngle(highlightIndex: Int): Float? {
    if (highlightIndex < 0) return null
    val ringIndex = ringSpecs.lastIndex
    return normalizeDegrees(highlightIndex * (360f / DOT_COUNT) + phaseOffsets[ringIndex] + ringRotations[ringIndex])
  }

  private fun easeOutCubic(value: Float): Float {
    val inverse = 1f - value
    return 1f - inverse * inverse * inverse
  }

  private fun resetRotations() {
    ringRotations.fill(0f)
    highlightIndex = -1
    highlightProgress = 0f
  }

  private fun normalizeDegrees(value: Float): Float {
    val normalized = value % 360f
    return if (normalized < 0f) normalized + 360f else normalized
  }

  private fun normalizeUnit(value: Float): Float {
    val normalized = value % 1f
    return if (normalized < 0f) normalized + 1f else normalized
  }

  private fun recycleBitmap(bitmap: Bitmap) {
    if (!bitmap.isRecycled) {
      bitmap.recycle()
    }
  }

  private fun invalidateOnAnimationFrame() {
    if (isAttachedToWindow) {
      postInvalidateOnAnimation()
    } else {
      invalidate()
    }
  }
}
