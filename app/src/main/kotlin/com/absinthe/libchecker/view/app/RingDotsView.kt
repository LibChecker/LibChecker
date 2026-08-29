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
import com.absinthe.libchecker.utils.UiUtils.toCircularBitmap
import com.absinthe.libchecker.utils.extensions.dpToDimension
import com.absinthe.rulesbundle.IconResMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
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
    private const val ROTATE_DURATION = 12000L
    private const val HIGHLIGHT_DURATION = 850L
    private const val HIGHLIGHT_HOLD_DURATION = 1250L
    private const val HIGHLIGHT_RING_PUSH = 0.09f
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
  private val gradientStops = intArrayOf(
    0xFF64D2FF.toInt(),
    0xFF0A84FF.toInt(),
    0xFFBF5AF2.toInt(),
    0xFFFF375F.toInt(),
    0xFFFF9F0A.toInt(),
    0xFF30D158.toInt()
  )
  private val gradientPositions = floatArrayOf(0f, 0.18f, 0.39f, 0.62f, 0.82f, 1f)
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
    val highlightAngle = resolveHighlightAngle()

    ringSpecs.forEachIndexed { ringIndex, spec ->
      drawRing(canvas, cx, cy, ringIndex, spec, highlightAngle)
    }
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
          ringSpecs.forEachIndexed { index, spec ->
            ringRotations[index] = normalizeDegrees(ringRotations[index] + delta * spec.rotationScale)
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
    if (elapsed >= cycleDuration) {
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
    pendingHighlightBitmap?.let(::recycleBitmap)
    pendingHighlightBitmap = null
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
      return icon.toCircularBitmap()
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
      return icon.toBitmap()
    }
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
    spec: RingSpec,
    highlightAngle: Float?
  ) {
    val dotCount = 24
    val angleStep = 360f / dotCount
    val baseRadius = ringRadii.getOrElse(ringIndex) { 0f }
    val phaseOffset = phaseOffsets.getOrElse(ringIndex) { 0f }
    val rotation = ringRotations.getOrElse(ringIndex) { 0f }
    paint.alpha = RING_ALPHA

    for (dotIndex in 0 until dotCount) {
      val baseAngle = dotIndex * angleStep
      val angle = normalizeDegrees(baseAngle + phaseOffset + rotation)
      val radiusScale = resolveRadiusScale(ringIndex, dotIndex, angle, highlightAngle, angleStep)
      val radius = baseRadius * radiusScale
      val angleRad = Math.toRadians(angle.toDouble())
      paint.color = sampleGradient(normalizeUnit(baseAngle / 360f + spec.colorShift))
      canvas.drawCircle(
        cx + (cos(angleRad) * radius).toFloat(),
        cy + (sin(angleRad) * radius).toFloat(),
        spec.dotRadiusPx,
        paint
      )
    }
    paint.alpha = 255
  }

  private fun resolveRadiusScale(
    ringIndex: Int,
    dotIndex: Int,
    angle: Float,
    highlightAngle: Float?,
    angleStep: Float
  ): Float {
    if (highlightAngle == null || highlightProgress <= 0f) return 1f
    val highlightRingIndex = ringSpecs.lastIndex
    val push = HIGHLIGHT_RING_PUSH * highlightProgress
    if (ringIndex == highlightRingIndex) {
      val distance = circularIndexDistance(dotIndex, highlightIndex, 24)
      return when (distance) {
        0 -> 1f + push
        1 -> 1f + push * 0.55f
        else -> 1f
      }
    }
    if (ringIndex == highlightRingIndex - 1) {
      val diff = abs(shortestAngleDistance(highlightAngle, angle))
      return 1f + push * 0.6f * computeInnerDisplacementWeight(diff, angleStep)
    }
    return 1f
  }

  private fun drawHighlight(canvas: Canvas, cx: Float, cy: Float) {
    val bitmap = currentHighlightBitmap?.takeUnless { it.isRecycled } ?: return
    if (highlightIndex < 0 || highlightProgress <= 0.12f) return

    val ringIndex = ringSpecs.lastIndex
    val spec = ringSpecs[ringIndex]
    val baseAngle = highlightIndex * (360f / 24f)
    val angle = normalizeDegrees(baseAngle + phaseOffsets[ringIndex] + ringRotations[ringIndex])
    val radius = ringRadii[ringIndex] * (1f + HIGHLIGHT_RING_PUSH * highlightProgress)
    val angleRad = Math.toRadians(angle.toDouble())
    val x = cx + (cos(angleRad) * radius).toFloat()
    val y = cy + (sin(angleRad) * radius).toFloat()
    val size = spec.dotRadiusPx * (1.1f + highlightProgress * 2.35f)
    val tint = sampleGradient(normalizeUnit(baseAngle / 360f + spec.colorShift))
    val alpha = (255 * highlightProgress).toInt().coerceIn(0, 255)

    paint.color = ColorUtils.setAlphaComponent(tint, (alpha * 0.24f).toInt())
    canvas.drawCircle(x, y, size * 1.22f, paint)
    paint.color = ColorUtils.setAlphaComponent(tint, (alpha * 0.62f).toInt())
    canvas.drawCircle(x, y, size, paint)

    val inset = size * 0.12f
    highlightDst.set(x - size + inset, y - size + inset, x + size - inset, y + size - inset)
    bitmapPaint.alpha = (170 + 85 * highlightProgress).toInt().coerceIn(0, 255)
    canvas.drawBitmap(bitmap, null, highlightDst, bitmapPaint)
    bitmapPaint.alpha = 255
  }

  private fun updateRingRadii() {
    val largestDot = ringSpecs.maxOf { it.dotRadiusPx }
    val outerRadius = (min(width, height) / 2f - largestDot * 1.5f).coerceAtLeast(0f)
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
    val angleStep = 360f / 24f
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
        (1f - easeOutQuint(progress)).coerceIn(0f, 1f)
      }
    }
  }

  private fun pickNextHighlight(previous: Int): Int {
    var next: Int
    do {
      next = Random.nextInt(24)
    } while (next == previous)
    return next
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

  private fun computeInnerDisplacementWeight(diffDegrees: Float, angleStep: Float): Float {
    val support = angleStep * 1.6f
    if (diffDegrees >= support) return 0f
    val normalized = (diffDegrees / support).coerceIn(0f, 1f)
    return ((cos(normalized * PI) + 1f) * 0.5f).toFloat()
  }

  private fun resolveHighlightAngle(): Float? {
    if (currentHighlightBitmap == null || highlightIndex < 0) return null
    val ringIndex = ringSpecs.lastIndex
    return normalizeDegrees(highlightIndex * (360f / 24f) + phaseOffsets[ringIndex] + ringRotations[ringIndex])
  }

  private fun circularIndexDistance(first: Int, second: Int, size: Int): Int {
    val direct = abs(first - second)
    return min(direct, size - direct)
  }

  private fun shortestAngleDistance(first: Float, second: Float): Float {
    var difference = (first - second) % 360f
    if (difference > 180f) difference -= 360f
    if (difference < -180f) difference += 360f
    return difference
  }

  private fun easeOutCubic(value: Float): Float {
    val inverse = 1f - value
    return 1f - inverse * inverse * inverse
  }

  private fun easeOutQuint(value: Float): Float {
    val inverse = 1f - value
    return 1f - inverse * inverse * inverse * inverse * inverse
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
