package com.absinthe.libchecker.view.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
import com.absinthe.libchecker.utils.extensions.dpToDimension
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import kotlin.coroutines.coroutineContext
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
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import timber.log.Timber

class RingDotsView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

  companion object {
    private const val MAX_HIGHLIGHT_QUEUE_SIZE = 3
    private const val HIGHLIGHT_RING_PUSH = 0.12f
  }

  fun interface HighlightIconEmitter {
    suspend fun emit(bitmap: Bitmap)
  }

  interface HighlightIconProvider {
    suspend fun produce(emitter: HighlightIconEmitter)
  }

  data class RingSpec(
    val radiusFraction: Float,
    val dotCount: Int,
    val dotRadiusPx: Float,
    val rotationScale: Float = 1f,
    val colorShift: Float = 0f,
    val phaseOffsetMultiplier: Float = 0f
  )

  private data class DotMeta(val ringIndex: Int, val indexInRing: Int)

  private var highlightRingIndex = 0

  var rotateDuration = 10000L
  var highlightDuration = 1000L
  var highlightHoldDuration = 1000L

  var ringSpecs: List<RingSpec> = createDefaultRingSpecs()
    set(value) {
      field = value.ifEmpty { createDefaultRingSpecs() }
      highlightRingIndex = resolveHighlightRingIndex(field).coerceAtLeast(0)
      ensureRingRotationState()
      invalidateRingBitmaps()
      rebuildDots(
        restartAnimations = isAttachedToWindow
      )
    }

  private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.FILL
    isFilterBitmap = true
  }
  private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    isFilterBitmap = true
  }
  private val ringBitmaps: MutableList<Bitmap> = mutableListOf()
  private var ringBitmapsDirty = true

  private val gradientStops = intArrayOf(
    0xFFFF9D5B.toInt(),
    0xFFFF6FB4.toInt(),
    0xFFB96BFF.toInt(),
    0xFF6296FF.toInt(),
    0xFF3AE4FF.toInt()
  )
  private val gradientPositions = floatArrayOf(0f, 0.28f, 0.54f, 0.78f, 1f)

  private var highlightIndex = -1
  private var highlightProgress = 0f
  private var dots: List<DotMeta> = emptyList()
  private var highlightCandidates: IntArray = intArrayOf()
  private var ringRotations: FloatArray = FloatArray(0)
  private var lastRotateAnimatorValue = 0f

  private val highlightBitmapQueue: BlockingQueue<Bitmap> =
    ArrayBlockingQueue(MAX_HIGHLIGHT_QUEUE_SIZE)
  private var currentHighlightBitmap: Bitmap? = null
  private var highlightIconProvider: HighlightIconProvider? = null
  private var iconProducerJob: Job? = null

  @Volatile
  private var highlightAnimationAvailable = false
  private var isRunning = false

  private val iconEmitter = object : HighlightIconEmitter {
    override suspend fun emit(bitmap: Bitmap) {
      coroutineContext.ensureActive()
      try {
        highlightBitmapQueue.put(bitmap)
        notifyBitmapQueueAvailable()
      } catch (interruption: InterruptedException) {
        Thread.currentThread().interrupt()
        throw CancellationException(
          "Interrupted while enqueuing highlight bitmap",
          interruption
        )
      }
    }
  }

  private var rotateAnim: ValueAnimator? = null
  private var highlightAnim: ValueAnimator? = null

  init {
    rebuildDots(restartAnimations = false)
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    invalidateRingBitmaps()
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    updateHighlightAvailability()
    if (isRunning) {
      startAnimations()
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    stopAnimations()
    stopIconProducer()
    clearHighlightQueue()
    invalidateRingBitmaps()
    updateHighlightAvailability()
  }

  private fun startAnimations() {
    Timber.d("startAnimations: $this")
    updateHighlightAvailability()
    if (!isRunning || !isAttachedToWindow) {
      return
    }
    if (dots.isEmpty() || highlightCandidates.isEmpty()) {
      stopAnimations()
      resetRingRotationState()
      highlightIndex = -1
      invalidate()
      return
    }

    ensureRingRotationState()
    ensureRotateAnimator()
    ensureHighlightAnimator()
  }

  private fun stopAnimations() {
    Timber.d("stopAnimations: $this")
    stopRotateAnimator()
    stopHighlightAnimator()
    resetRingRotationState()
    updateHighlightAvailability()
  }

  private fun ensureRotateAnimator() {
    if (rotateAnim == null) {
      lastRotateAnimatorValue = 0f
      rotateAnim = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = rotateDuration
        interpolator = LinearInterpolator()
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener {
          val value = it.animatedValue as Float
          var delta = value - lastRotateAnimatorValue
          if (delta < 0f) {
            delta += 360f
          }
          if (delta != 0f) {
            ensureRingRotationState()
            val rotations = ringRotations
            ringSpecs.forEachIndexed { index, spec ->
              val updated = rotations[index] + delta * spec.rotationScale
              rotations[index] = normalizeDegrees(updated)
            }
          }
          lastRotateAnimatorValue = value
          invalidate()
        }
        start()
      }
    } else if (rotateAnim?.isStarted == false) {
      lastRotateAnimatorValue = rotateAnim?.animatedValue as? Float ?: 0f
      rotateAnim?.start()
    }
  }

  private fun stopRotateAnimator() {
    rotateAnim?.apply {
      removeAllUpdateListeners()
      cancel()
    }
    rotateAnim = null
    lastRotateAnimatorValue = 0f
  }

  private fun ensureHighlightAnimator() {
    val provider = highlightIconProvider
    updateHighlightAvailability()
    if (!shouldHighlightAnimate()) {
      stopHighlightAnimator()
      return
    }

    provider?.let { startIconProducerJob(it) }

    if (highlightAnim == null) {
      if (currentHighlightBitmap == null) {
        currentHighlightBitmap = highlightBitmapQueue.poll()
        if (currentHighlightBitmap == null) {
          provider?.let { startIconProducerJob(it) }
          return
        }
      }
      highlightIndex = pickNextHighlight(exclude = -1)
      highlightAnim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = (highlightDuration + highlightHoldDuration).coerceAtLeast(1L)
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.RESTART
        addListener(object : AnimatorListenerAdapter() {
          override fun onAnimationRepeat(animation: Animator) {
            if (highlightCandidates.isEmpty()) return
            val previous = highlightIndex
            highlightIndex = pickNextHighlight(previous)
            if (!advanceHighlightBitmap()) {
              stopHighlightAnimator()
            }
          }
        })
        addUpdateListener {
          highlightProgress = computeHighlightProgress(it.animatedFraction)
          invalidate()
        }
        start()
      }
    } else if (highlightAnim?.isStarted == false) {
      highlightAnim?.start()
    }
  }

  private fun stopHighlightAnimator() {
    highlightAnim?.apply {
      removeAllListeners()
      removeAllUpdateListeners()
      cancel()
    }
    highlightAnim = null
    highlightProgress = 0f
    highlightIndex = -1
    currentHighlightBitmap = null
    invalidate()
  }

  private fun shouldHighlightAnimate(): Boolean {
    if (!isRunning) return false
    if (!highlightAnimationAvailable) return false
    val hasBitmap = currentHighlightBitmap != null || !highlightBitmapQueue.isEmpty()
    return hasBitmap
  }

  private fun advanceHighlightBitmap(): Boolean {
    highlightIconProvider?.let { startIconProducerJob(it) }
    val next = highlightBitmapQueue.poll()
    currentHighlightBitmap = next
    return next != null
  }

  private fun notifyBitmapQueueAvailable() {
    post {
      if (!isAttachedToWindow || !isRunning) return@post
      highlightIconProvider?.let { startIconProducerJob(it) }
      ensureHighlightAnimator()
    }
  }

  private fun startIconProducerJob(provider: HighlightIconProvider) {
    if (!isRunning) return
    if (iconProducerJob?.isActive == true) return
    val job = CoroutineScope(Dispatchers.IO).launch {
      try {
        provider.produce(iconEmitter)
      } catch (_: CancellationException) {
        // Ignored, coroutine cancelled.
      } catch (_: Exception) {
        // Ignore producer failures to keep UI responsive.
      }
    }
    job.invokeOnCompletion {
      iconProducerJob = null
    }
    iconProducerJob = job
  }

  private fun stopIconProducer() {
    iconProducerJob?.cancel()
    iconProducerJob = null
  }

  private fun clearHighlightQueue() {
    highlightBitmapQueue.clear()
    currentHighlightBitmap = null
  }

  private fun updateHighlightAvailability() {
    highlightAnimationAvailable =
      isAttachedToWindow && dots.isNotEmpty() && highlightCandidates.isNotEmpty()
  }

  private fun drawDynamicRing(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    spec: RingSpec,
    baseRadius: Float,
    phaseOffset: Float,
    rotation: Float,
    radiusProvider: (Int) -> Float = { baseRadius }
  ) {
    if (spec.dotCount <= 0 || baseRadius <= 0f) return
    val originalShader = paint.shader
    val originalColor = paint.color
    val originalAlpha = paint.alpha

    canvas.withTranslation(cx, cy) {
      rotate(rotation)
      val angleStep = 360f / spec.dotCount
      for (dotIndex in 0 until spec.dotCount) {
        val baseAngle = dotIndex * angleStep
        val angleDeg = baseAngle + phaseOffset
        val angleRad = Math.toRadians(angleDeg.toDouble())
        val resolvedRadius = radiusProvider(dotIndex).coerceAtLeast(0f)
        val x = (cos(angleRad) * resolvedRadius).toFloat()
        val y = (sin(angleRad) * resolvedRadius).toFloat()

        val angleFraction = baseAngle / 360f
        val halfStepFraction = (angleStep / 360f) * 0.5f
        val shift = spec.colorShift
        val startColor = sampleGradient(normalizeUnit(angleFraction - halfStepFraction + shift))
        val endColor = sampleGradient(normalizeUnit(angleFraction + halfStepFraction + shift))

        val tangentAngle = angleRad + PI / 2
        val cosTangent = cos(tangentAngle)
        val sinTangent = sin(tangentAngle)
        val halfSpan = spec.dotRadiusPx * 1.3f
        val startX = (x - cosTangent * halfSpan).toFloat()
        val startY = (y - sinTangent * halfSpan).toFloat()
        val endX = (x + cosTangent * halfSpan).toFloat()
        val endY = (y + sinTangent * halfSpan).toFloat()

        val shader = LinearGradient(startX, startY, endX, endY, startColor, endColor, Shader.TileMode.CLAMP)
        paint.shader = shader
        drawCircle(x, y, spec.dotRadiusPx, paint)
        paint.shader = null
      }
    }

    paint.shader = originalShader
    paint.color = originalColor
    paint.alpha = originalAlpha
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    if (dots.isEmpty() || width == 0 || height == 0) return

    val cx = width / 2f
    val cy = height / 2f
    val maxRadius = min(width, height) / 2f
    val largestDot = ringSpecs.maxOfOrNull { it.dotRadiusPx } ?: 0f
    val radiusLimit = max(0f, maxRadius - largestDot * 1.5f)
    val radii = computeRingRadii(radiusLimit)
    val phaseOffsets = computePhaseOffsets()

    ensureRingBitmaps(width, height, radii, phaseOffsets)

    val highlightBitmap = currentHighlightBitmap
    val highlightMeta = dots.getOrNull(highlightIndex)
    val highlightRingIdx = highlightMeta?.ringIndex

    var highlightAngleDeg = 0f
    val highlightScaleMap: Map<Int, Float> = if (highlightMeta != null && highlightRingIdx != null) {
      val spec = ringSpecs.getOrNull(highlightRingIdx)
      val dotCount = spec?.dotCount ?: 0
      if (spec == null || dotCount <= 0) {
        emptyMap()
      } else {
        val basePhase = phaseOffsets.getOrNull(highlightRingIdx) ?: 0f
        val rotation = ringRotations.getOrNull(highlightRingIdx) ?: 0f
        val angleStep = 360f / dotCount
        val baseAngle = highlightMeta.indexInRing * angleStep
        highlightAngleDeg = normalizeDegrees(baseAngle + basePhase + rotation)

        val push = (HIGHLIGHT_RING_PUSH * highlightProgress).coerceAtLeast(0f)
        if (push == 0f) {
          emptyMap()
        } else {
          val center = wrapIndex(highlightMeta.indexInRing, dotCount)
          val map = mutableMapOf<Int, Float>()
          map[center] = 1f + push
          if (dotCount > 1) {
            val left = wrapIndex(center - 1, dotCount)
            val right = wrapIndex(center + 1, dotCount)
            if (left != center) {
              map[left] = 1f + push * 0.6f
            }
            if (right != center && right != left) {
              map[right] = 1f + push * 0.6f
            }
          }
          map
        }
      }
    } else {
      emptyMap()
    }

    val innerRingIdx = highlightRingIdx?.minus(1)?.takeIf { it >= 0 }
    val innerPushStrength = if (innerRingIdx != null) {
      (HIGHLIGHT_RING_PUSH * 0.6f * highlightProgress).coerceAtLeast(0f)
    } else {
      0f
    }

    ringSpecs.forEachIndexed { index, spec ->
      val radiusBase = radii.getOrNull(index) ?: 0f
      val phaseOffset = phaseOffsets.getOrNull(index) ?: 0f
      val rotation = ringRotations.getOrNull(index) ?: 0f
      val isHighlightRing = highlightRingIdx == index && spec.dotCount > 0
      val angleStep = if (spec.dotCount > 0) 360f / spec.dotCount else 0f
      val isInnerAffectedRing = innerRingIdx == index && spec.dotCount > 0 && innerPushStrength > 0f
      val shouldDynamicHighlight = isHighlightRing && highlightScaleMap.isNotEmpty()

      when {
        shouldDynamicHighlight -> {
          val resolver: (Int) -> Float = { dotIndex ->
            val scale = highlightScaleMap[dotIndex] ?: 1f
            radiusBase * scale
          }
          drawDynamicRing(canvas, cx, cy, spec, radiusBase, phaseOffset, rotation, resolver)
        }

        isInnerAffectedRing -> {
          val resolver: (Int) -> Float = { dotIndex ->
            val dotAngle = normalizeDegrees(phaseOffset + dotIndex * angleStep + rotation)
            val diff = shortestAngleDistance(highlightAngleDeg, dotAngle)
            val weight = computeInnerDisplacementWeight(abs(diff), angleStep)
            val scale = 1f + innerPushStrength * weight
            radiusBase * scale
          }
          drawDynamicRing(canvas, cx, cy, spec, radiusBase, phaseOffset, rotation, resolver)
        }

        else -> {
          val bitmap = ringBitmaps.getOrNull(index)
          if (bitmap != null && !bitmap.isRecycled) {
            canvas.withTranslation(cx, cy) {
              rotate(rotation)
              translate(-cx, -cy)
              drawBitmap(bitmap, 0f, 0f, bitmapPaint)
            }
          }
        }
      }
    }

    if (highlightMeta != null && highlightBitmap != null) {
      val spec = ringSpecs.getOrNull(highlightMeta.ringIndex)
      if (spec != null && spec.dotCount > 0) {
        val angleStep = 360f / spec.dotCount
        val baseAngle = highlightMeta.indexInRing * angleStep
        val phaseOffset = phaseOffsets.getOrNull(highlightMeta.ringIndex) ?: 0f
        val radiusBase = radii.getOrNull(highlightMeta.ringIndex) ?: 0f
        val radiusScale = if (highlightMeta.ringIndex == highlightRingIdx) {
          highlightScaleMap[wrapIndex(highlightMeta.indexInRing, spec.dotCount)] ?: 1f
        } else {
          1f
        }
        val rotation = ringRotations.getOrNull(highlightMeta.ringIndex) ?: 0f
        val angleDeg = baseAngle + phaseOffset + rotation
        val angleRad = Math.toRadians(angleDeg.toDouble())
        val radius = radiusBase * radiusScale
        val size = spec.dotRadiusPx * (1.1f + highlightProgress * 2.6f)
        val x = cx + cos(angleRad) * radius
        val y = cy + sin(angleRad) * radius
        if (highlightProgress > 0.12f) {
          val tint = sampleGradient(normalizeUnit(baseAngle / 360f + spec.colorShift))
          drawHighlighted(
            canvas,
            highlightBitmap,
            tint,
            x.toFloat(),
            y.toFloat(),
            size,
            highlightProgress
          )
        }
      }
    }
  }

  private fun ensureRingBitmaps(
    width: Int,
    height: Int,
    radii: FloatArray,
    phaseOffsets: FloatArray
  ) {
    val needsRebuild = ringBitmapsDirty ||
      ringBitmaps.size != ringSpecs.size ||
      ringBitmaps.any { it.width != width || it.height != height }

    if (!needsRebuild) {
      return
    }

    clearRingBitmaps()
    if (width <= 0 || height <= 0) return

    val cx = width / 2f
    val cy = height / 2f
    val basePaint = paint
    basePaint.alpha = 255

    ringSpecs.forEachIndexed { index, spec ->
      if (spec.dotCount <= 0) {
        ringBitmaps.add(createBitmap(width, height))
        return@forEachIndexed
      }

      val bitmap = createBitmap(width, height)
      val bitmapCanvas = Canvas(bitmap)
      val radius = radii.getOrNull(index) ?: 0f
      val phaseOffset = phaseOffsets.getOrNull(index) ?: 0f
      val angleStep = 360f / spec.dotCount

      for (dotIndex in 0 until spec.dotCount) {
        val baseAngle = dotIndex * angleStep
        val angleDeg = baseAngle + phaseOffset
        val angleRad = Math.toRadians(angleDeg.toDouble())
        val x = cx + cos(angleRad) * radius
        val y = cy + sin(angleRad) * radius

        val angleFraction = baseAngle / 360f
        val halfStepFraction = (angleStep / 360f) * 0.5f
        val shift = spec.colorShift
        val startColor = sampleGradient(normalizeUnit(angleFraction - halfStepFraction + shift))
        val endColor = sampleGradient(normalizeUnit(angleFraction + halfStepFraction + shift))

        val tangentAngle = angleRad + PI / 2
        val cosTangent = cos(tangentAngle)
        val sinTangent = sin(tangentAngle)
        val halfSpan = spec.dotRadiusPx * 1.3f
        val startX = x.toFloat() - (cosTangent * halfSpan).toFloat()
        val startY = y.toFloat() - (sinTangent * halfSpan).toFloat()
        val endX = x.toFloat() + (cosTangent * halfSpan).toFloat()
        val endY = y.toFloat() + (sinTangent * halfSpan).toFloat()

        val shader =
          LinearGradient(startX, startY, endX, endY, startColor, endColor, Shader.TileMode.CLAMP)
        basePaint.shader = shader
        bitmapCanvas.drawCircle(x.toFloat(), y.toFloat(), spec.dotRadiusPx, basePaint)
        basePaint.shader = null
      }

      ringBitmaps.add(bitmap)
    }

    ringBitmapsDirty = false
  }

  private fun rebuildDots(restartAnimations: Boolean) {
    invalidateRingBitmaps()
    ensureRingRotationState()
    val ringCount = ringSpecs.size
    val targetRingIndex = when {
      ringCount <= 0 -> -1

      else -> {
        val resolved = resolveHighlightRingIndex(ringSpecs)
        val fallback = highlightRingIndex
        (if (resolved >= 0) resolved else fallback).coerceIn(0, ringCount - 1)
      }
    }
    highlightRingIndex = targetRingIndex.coerceAtLeast(0)
    val newDots = mutableListOf<DotMeta>()
    val highlightHolders = mutableListOf<Int>()
    ringSpecs.forEachIndexed { ringIndex, spec ->
      if (spec.dotCount <= 0) return@forEachIndexed
      repeat(spec.dotCount) { position ->
        val meta = DotMeta(ringIndex, position)
        newDots.add(meta)
        if (ringIndex == targetRingIndex) {
          highlightHolders.add(newDots.lastIndex)
        }
      }
    }
    dots = newDots
    highlightCandidates = highlightHolders.toIntArray()

    highlightIndex = when {
      highlightCandidates.isEmpty() -> -1
      highlightCandidates.any { it == highlightIndex } -> highlightIndex
      highlightCandidates.isNotEmpty() -> highlightCandidates[0]
      else -> -1
    }

    updateHighlightAvailability()

    if (restartAnimations) {
      stopAnimations()
      if (isRunning) {
        startAnimations()
      } else {
        invalidate()
      }
    } else {
      invalidate()
    }
  }

  private fun resolveHighlightRingIndex(specs: List<RingSpec>): Int {
    if (specs.isEmpty()) return -1
    return specs.withIndex()
      .maxWithOrNull(compareBy({ it.value.radiusFraction }, { it.index }))
      ?.index
      ?: specs.lastIndex
  }

  private fun computeHighlightProgress(fraction: Float): Float {
    if (fraction <= 0f || fraction >= 1f) return 0f

    val activeDuration = highlightDuration.coerceAtLeast(0L).toFloat()
    if (activeDuration <= 0f) return 0f

    val holdDuration = highlightHoldDuration.coerceAtLeast(0L).toFloat()
    val totalDuration = activeDuration + holdDuration
    if (totalDuration <= 0f) return 0f

    val growPortion = 0.3f
    val growDuration = (activeDuration * growPortion).coerceIn(1f, activeDuration)
    val shrinkDuration = (activeDuration - growDuration).coerceAtLeast(0f)

    val elapsed = (fraction * totalDuration).coerceIn(0f, totalDuration)

    return when {
      elapsed <= growDuration -> {
        val t = (elapsed / growDuration).coerceIn(0f, 1f)
        easeOutCubic(t)
      }

      elapsed <= growDuration + holdDuration -> 1f

      else -> {
        if (shrinkDuration <= 0f) {
          0f
        } else {
          val shrinkElapsed = (elapsed - growDuration - holdDuration).coerceAtLeast(0f)
          val t = (shrinkElapsed / shrinkDuration).coerceIn(0f, 1f)
          (1f - easeOutQuint(t)).coerceIn(0f, 1f)
        }
      }
    }
  }

  private fun easeOutCubic(t: Float): Float {
    val inv = 1f - t
    return 1f - inv * inv * inv
  }

  private fun easeOutQuint(t: Float): Float {
    val inv = 1f - t
    return 1f - inv * inv * inv * inv * inv
  }

  private fun pickNextHighlight(exclude: Int): Int {
    if (highlightCandidates.isEmpty()) return -1
    if (highlightCandidates.size == 1) return highlightCandidates[0]
    var next: Int
    do {
      next = highlightCandidates[Random.nextInt(highlightCandidates.size)]
    } while (next == exclude)
    return next
  }

  private fun sampleGradient(position: Float): Int {
    if (gradientStops.size <= 1) return gradientStops.firstOrNull() ?: 0xFFFFFFFF.toInt()
    val p = ((position % 1f) + 1f) % 1f
    for (i in 0 until gradientStops.lastIndex) {
      val start = gradientPositions.getOrNull(i) ?: 0f
      val end = gradientPositions.getOrNull(i + 1) ?: 1f
      if (p <= end) {
        val span = max(end - start, 0.0001f)
        val t = ((p - start) / span).coerceIn(0f, 1f)
        return ColorUtils.blendARGB(gradientStops[i], gradientStops[i + 1], t)
      }
    }
    return gradientStops.last()
  }

  private fun drawHighlighted(
    canvas: Canvas,
    bitmap: Bitmap?,
    tint: Int,
    x: Float,
    y: Float,
    size: Float,
    progress: Float
  ) {
    val alpha = (255 * progress).toInt().coerceIn(0, 255)

    val originalColor = paint.color
    val originalAlpha = paint.alpha

    paint.color = ColorUtils.setAlphaComponent(tint, (alpha * 0.9f).toInt())
    canvas.drawCircle(x, y, size, paint)

    bitmap?.let {
      val inset = size * 0.12f
      val dst = RectF(x - size + inset, y - size + inset, x + size - inset, y + size - inset)
      paint.alpha = (170 + 85 * progress).toInt().coerceIn(0, 255)
      canvas.drawBitmap(it, null, dst, paint)
      paint.alpha = 255
    }

    paint.color = originalColor
    paint.alpha = originalAlpha
  }

  private fun circularDistance(a: Int, b: Int, count: Int): Int {
    if (count <= 0 || a < 0 || b < 0) return Int.MAX_VALUE
    val diff = abs(a - b)
    return min(diff, count - diff)
  }

  private fun wrapIndex(index: Int, size: Int): Int {
    if (size <= 0) return 0
    var result = index % size
    if (result < 0) {
      result += size
    }
    return result
  }

  private fun shortestAngleDistance(a: Float, b: Float): Float {
    var diff = (a - b) % 360f
    if (diff > 180f) {
      diff -= 360f
    } else if (diff < -180f) {
      diff += 360f
    }
    return diff
  }

  private fun computeInnerDisplacementWeight(diffDegrees: Float, angleStep: Float): Float {
    if (angleStep <= 0f) return 0f
    val support = angleStep * 1.6f
    if (diffDegrees >= support) return 0f
    val normalized = (diffDegrees / support).coerceIn(0f, 1f)
    return ((cos(normalized * PI) + 1f) * 0.5f).toFloat()
  }

  fun setIconBitmap(bitmap: Bitmap?) {
    stopIconProducer()
    highlightIconProvider = null
    stopHighlightAnimator()
    clearHighlightQueue()
    if (bitmap == null) {
      invalidate()
      return
    }

    if (!highlightBitmapQueue.offer(bitmap)) {
      highlightBitmapQueue.clear()
      highlightBitmapQueue.offer(bitmap)
    }
    notifyBitmapQueueAvailable()
  }

  fun setHighlightIconProvider(provider: HighlightIconProvider?) {
    if (highlightIconProvider === provider) return

    stopHighlightAnimator()
    stopIconProducer()
    clearHighlightQueue()
    highlightIconProvider = provider
    updateHighlightAvailability()

    if (provider != null && isAttachedToWindow && isRunning) {
      startIconProducerJob(provider)
    }

    if (isAttachedToWindow && isRunning) {
      ensureHighlightAnimator()
    } else {
      invalidate()
    }
  }

  fun isHighlightAnimationAvailable(): Boolean = highlightAnimationAvailable

  fun start() {
    if (isRunning) return
    isRunning = true
    updateHighlightAvailability()
    if (isAttachedToWindow) {
      startAnimations()
      highlightIconProvider?.let { startIconProducerJob(it) }
    }
  }

  fun stop() {
    if (!isRunning && rotateAnim == null && highlightAnim == null) return
    isRunning = false
    stopAnimations()
    stopIconProducer()
    updateHighlightAvailability()
    stopIconProducer()
  }

  private fun invalidateRingBitmaps() {
    ringBitmapsDirty = true
    clearRingBitmaps()
  }

  private fun clearRingBitmaps() {
    if (ringBitmaps.isEmpty()) return
    ringBitmaps.forEach { bmp ->
      if (!bmp.isRecycled) {
        bmp.recycle()
      }
    }
    ringBitmaps.clear()
  }

  private fun ensureRingRotationState() {
    if (ringRotations.size == ringSpecs.size) return
    val previous = ringRotations
    ringRotations = FloatArray(ringSpecs.size) { index ->
      previous.getOrNull(index) ?: 0f
    }
  }

  private fun resetRingRotationState() {
    ensureRingRotationState()
    for (index in ringRotations.indices) {
      ringRotations[index] = 0f
    }
  }

  private fun normalizeDegrees(value: Float): Float {
    var result = value % 360f
    if (result < 0f) {
      result += 360f
    }
    return result
  }

  private fun normalizeUnit(value: Float): Float {
    var result = value % 1f
    if (result < 0f) {
      result += 1f
    }
    return result
  }

  private fun createDefaultRingSpecs(): List<RingSpec> {
    val count = 24
    return listOf(
      RingSpec(
        0.62f,
        count,
        context.dpToDimension(2.4f),
        rotationScale = 1f,
        colorShift = -0.02f,
        phaseOffsetMultiplier = 0f
      ),
      RingSpec(
        0.72f,
        count,
        context.dpToDimension(3.9f),
        rotationScale = 1f,
        colorShift = 0.04f,
        phaseOffsetMultiplier = 0f
      ),
      RingSpec(
        0.85f,
        count,
        context.dpToDimension(3.4f),
        rotationScale = 1f,
        colorShift = 0.08f,
        phaseOffsetMultiplier = 0f
      ),
      RingSpec(
        0.9f,
        count,
        context.dpToDimension(5.6f),
        rotationScale = 1f,
        colorShift = 0.12f,
        phaseOffsetMultiplier = 0f
      )
    )
  }

  private fun computeRingRadii(outerRadius: Float): FloatArray {
    if (ringSpecs.isEmpty()) return FloatArray(0)
    val radii = FloatArray(ringSpecs.size)
    var nextRadius = outerRadius
    for (index in ringSpecs.lastIndex downTo 0) {
      val spec = ringSpecs[index]
      val gap = spec.dotRadiusPx * 1.05f
      val desired = if (index == ringSpecs.lastIndex) {
        outerRadius
      } else {
        (outerRadius * spec.radiusFraction).coerceAtMost(max(0f, nextRadius - gap))
      }
      val radius = desired.coerceAtLeast(0f)
      radii[index] = radius
      nextRadius = radius
    }
    return radii
  }

  private fun computePhaseOffsets(): FloatArray {
    if (ringSpecs.isEmpty()) return FloatArray(0)
    val offsets = FloatArray(ringSpecs.size)
    var outerOffset = 0f
    var outerAngleStep = 0f
    for (index in ringSpecs.lastIndex downTo 0) {
      val spec = ringSpecs[index]
      val angleStep = if (spec.dotCount > 0) 360f / spec.dotCount else 0f
      val baseOffset = if (index == ringSpecs.lastIndex) {
        0f
      } else {
        outerOffset + outerAngleStep / 2f
      }
      val additional = angleStep * spec.phaseOffsetMultiplier
      val resolved = normalizeDegrees(baseOffset + additional)
      offsets[index] = resolved
      outerOffset = resolved
      outerAngleStep = angleStep
    }
    return offsets
  }
}
