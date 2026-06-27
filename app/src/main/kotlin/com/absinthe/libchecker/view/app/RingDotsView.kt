package com.absinthe.libchecker.view.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.withTranslation
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.UiUtils.toCircularBitmap
import com.absinthe.libchecker.utils.extensions.dpToDimension
import com.absinthe.rulesbundle.IconResMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

class RingDotsView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

  companion object {
    private const val MAX_HIGHLIGHT_QUEUE_SIZE = 3
    private const val HIGHLIGHT_RING_PUSH = 0.09f
    private const val MAX_HIGHLIGHT_BITMAP_BYTES = 150 * 1024 * 1024
    private const val RING_ALPHA = 232
    private const val RULE_ICON_POOL_COUNT = 100
    private const val HIGHLIGHT_PROVIDER_MAX_MISSES = 8
    private const val HIGHLIGHT_PROVIDER_RETRY_DELAY_MS = 120L
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
  private data class RingBitmap(val bitmap: Bitmap)
  private data class HighlightBitmap(val bitmap: Bitmap, val recyclable: Boolean)

  private var highlightRingIndex = 0

  var rotateDuration = 12000L
  var highlightDuration = 850L
  var highlightHoldDuration = 1250L

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
  private val highlightDst = RectF()
  private val ringBitmaps: MutableList<RingBitmap?> = mutableListOf()
  private var ringBitmapsDirty = true
  private var geometryDirty = true
  private var cachedRingRadii = FloatArray(0)
  private var cachedPhaseOffsets = FloatArray(0)
  private var highlightRadiusScales = FloatArray(0)

  private val gradientStops = intArrayOf(
    0xFF64D2FF.toInt(),
    0xFF0A84FF.toInt(),
    0xFFBF5AF2.toInt(),
    0xFFFF375F.toInt(),
    0xFFFF9F0A.toInt(),
    0xFF30D158.toInt()
  )
  private val gradientPositions = floatArrayOf(0f, 0.18f, 0.39f, 0.62f, 0.82f, 1f)

  private var highlightIndex = -1
  private var highlightProgress = 0f
  private var dots: List<DotMeta> = emptyList()
  private var highlightCandidates: IntArray = intArrayOf()
  private var ringRotations: FloatArray = FloatArray(0)
  private var lastRotateAnimatorValue = 0f

  private val highlightBitmapChannel = Channel<HighlightBitmap>(MAX_HIGHLIGHT_QUEUE_SIZE)
  private var currentHighlightBitmap: HighlightBitmap? = null
  private var highlightIconProvider: HighlightIconProvider? = null
  private var iconProducerJob: Job? = null

  @Volatile
  private var highlightAnimationAvailable = false

  @Volatile
  private var isRunning = false

  private val iconEmitter = HighlightIconEmitter { bitmap ->
    currentCoroutineContext().ensureActive()
    // See also: RecordingCanvas#throwIfCannotDraw
    if (bitmap.isRecycled || bitmap.byteCount > MAX_HIGHLIGHT_BITMAP_BYTES) {
      recycleBitmap(bitmap)
      return@HighlightIconEmitter
    }
    if (!isRunning || !highlightAnimationAvailable) {
      recycleBitmap(bitmap)
      return@HighlightIconEmitter
    }
    val highlightBitmap = HighlightBitmap(bitmap, recyclable = true)
    try {
      highlightBitmapChannel.send(highlightBitmap)
      notifyBitmapQueueAvailable()
    } catch (cancellation: CancellationException) {
      recycleHighlightBitmap(highlightBitmap)
      throw cancellation
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
    clearHighlightBitmaps()
    invalidateRingBitmaps()
    updateHighlightAvailability()
  }

  private fun startAnimations() {
    updateHighlightAvailability()
    if (!isRunning || !isAttachedToWindow) {
      return
    }
    if (dots.isEmpty() || highlightCandidates.isEmpty()) {
      stopAnimations()
      resetRingRotationState()
      highlightIndex = -1
      invalidateOnAnimationFrame()
      return
    }

    ensureRingRotationState()
    ensureRotateAnimator()
    ensureHighlightAnimator()
  }

  private fun stopAnimations() {
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
          invalidateOnAnimationFrame()
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
        currentHighlightBitmap = pollHighlightBitmap()
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
          invalidateOnAnimationFrame()
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
    clearCurrentHighlightBitmap()
    invalidateOnAnimationFrame()
  }

  private fun shouldHighlightAnimate(): Boolean {
    return isRunning && highlightAnimationAvailable
  }

  private fun advanceHighlightBitmap(): Boolean {
    highlightIconProvider?.let { startIconProducerJob(it) }
    clearCurrentHighlightBitmap()
    currentHighlightBitmap = pollHighlightBitmap()
    return currentHighlightBitmap != null
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

  private fun pollHighlightBitmap(): HighlightBitmap? {
    return highlightBitmapChannel.tryReceive().getOrNull()
  }

  private fun clearHighlightBitmaps() {
    clearQueuedHighlightBitmaps()
    clearCurrentHighlightBitmap()
  }

  private fun clearQueuedHighlightBitmaps() {
    while (true) {
      val bitmap = highlightBitmapChannel.tryReceive().getOrNull() ?: break
      recycleHighlightBitmap(bitmap)
    }
  }

  private fun clearCurrentHighlightBitmap() {
    currentHighlightBitmap?.let(::recycleHighlightBitmap)
    currentHighlightBitmap = null
  }

  private fun recycleHighlightBitmap(bitmap: HighlightBitmap) {
    if (bitmap.recyclable) {
      recycleBitmap(bitmap.bitmap)
    }
  }

  private fun recycleBitmap(bitmap: Bitmap) {
    if (!bitmap.isRecycled) {
      bitmap.recycle()
    }
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
    radiusScales: FloatArray? = null,
    highlightAngleDeg: Float = 0f,
    innerPushStrength: Float = 0f
  ) {
    if (spec.dotCount <= 0 || baseRadius <= 0f) return
    val originalColor = paint.color
    val originalAlpha = paint.alpha

    canvas.withTranslation(cx, cy) {
      rotate(rotation)
      val angleStep = 360f / spec.dotCount
      for (dotIndex in 0 until spec.dotCount) {
        val baseAngle = dotIndex * angleStep
        val angleDeg = baseAngle + phaseOffset
        val angleRad = Math.toRadians(angleDeg.toDouble())
        val radiusScale = when {
          radiusScales != null -> radiusScales[dotIndex]

          innerPushStrength > 0f -> {
            val dotAngle = normalizeDegrees(angleDeg + rotation)
            val diff = shortestAngleDistance(highlightAngleDeg, dotAngle)
            val weight = computeInnerDisplacementWeight(abs(diff), angleStep)
            1f + innerPushStrength * weight
          }

          else -> 1f
        }
        val resolvedRadius = (baseRadius * radiusScale).coerceAtLeast(0f)
        val x = (cos(angleRad) * resolvedRadius).toFloat()
        val y = (sin(angleRad) * resolvedRadius).toFloat()

        val angleFraction = baseAngle / 360f
        paint.color = sampleGradient(normalizeUnit(angleFraction + spec.colorShift))
        paint.alpha = RING_ALPHA
        drawCircle(x, y, spec.dotRadiusPx, paint)
      }
    }

    paint.color = originalColor
    paint.alpha = originalAlpha
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    if (dots.isEmpty() || width == 0 || height == 0) return

    val cx = width / 2f
    val cy = height / 2f
    ensureGeometryCache()
    val radii = cachedRingRadii
    val phaseOffsets = cachedPhaseOffsets

    ensureRingBitmaps(radii, phaseOffsets)

    val highlightBitmap = currentHighlightBitmap?.bitmap?.takeIf { !it.isRecycled }
    val highlightMeta = dots.getOrNull(highlightIndex)
    val highlightRingIdx = highlightMeta?.ringIndex

    var highlightAngleDeg = 0f
    val highlightScales = if (highlightMeta != null && highlightRingIdx != null) {
      val spec = ringSpecs.getOrNull(highlightRingIdx)
      val dotCount = spec?.dotCount ?: 0
      if (spec == null || dotCount <= 0) {
        null
      } else {
        val basePhase = phaseOffsets.getOrNull(highlightRingIdx) ?: 0f
        val rotation = ringRotations.getOrNull(highlightRingIdx) ?: 0f
        val angleStep = 360f / dotCount
        val baseAngle = highlightMeta.indexInRing * angleStep
        highlightAngleDeg = normalizeDegrees(baseAngle + basePhase + rotation)

        val push = (HIGHLIGHT_RING_PUSH * highlightProgress).coerceAtLeast(0f)
        if (push == 0f) {
          null
        } else {
          prepareHighlightRadiusScales(dotCount, highlightMeta.indexInRing, push)
        }
      }
    } else {
      null
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
      val isInnerAffectedRing = innerRingIdx == index && spec.dotCount > 0 && innerPushStrength > 0f
      val shouldDynamicHighlight = isHighlightRing && highlightScales != null

      when {
        shouldDynamicHighlight -> {
          drawDynamicRing(
            canvas = canvas,
            cx = cx,
            cy = cy,
            spec = spec,
            baseRadius = radiusBase,
            phaseOffset = phaseOffset,
            rotation = rotation,
            radiusScales = highlightScales
          )
        }

        isInnerAffectedRing -> {
          drawDynamicRing(
            canvas = canvas,
            cx = cx,
            cy = cy,
            spec = spec,
            baseRadius = radiusBase,
            phaseOffset = phaseOffset,
            rotation = rotation,
            highlightAngleDeg = highlightAngleDeg,
            innerPushStrength = innerPushStrength
          )
        }

        else -> {
          val ringBitmap = ringBitmaps.getOrNull(index)
          val bitmap = ringBitmap?.bitmap
          if (bitmap != null && !bitmap.isRecycled) {
            canvas.withTranslation(cx, cy) {
              rotate(rotation)
              drawBitmap(bitmap, -bitmap.width / 2f, -bitmap.height / 2f, bitmapPaint)
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
          highlightScales?.get(wrapIndex(highlightMeta.indexInRing, spec.dotCount)) ?: 1f
        } else {
          1f
        }
        val rotation = ringRotations.getOrNull(highlightMeta.ringIndex) ?: 0f
        val angleDeg = baseAngle + phaseOffset + rotation
        val angleRad = Math.toRadians(angleDeg.toDouble())
        val radius = radiusBase * radiusScale
        val size = spec.dotRadiusPx * (1.1f + highlightProgress * 2.35f)
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

  private fun ensureGeometryCache() {
    if (!geometryDirty && cachedRingRadii.size == ringSpecs.size && cachedPhaseOffsets.size == ringSpecs.size) {
      return
    }

    val maxRadius = min(width, height) / 2f
    val largestDot = ringSpecs.maxOfOrNull { it.dotRadiusPx } ?: 0f
    val radiusLimit = max(0f, maxRadius - largestDot * 1.5f)
    cachedRingRadii = computeRingRadii(radiusLimit)
    cachedPhaseOffsets = computePhaseOffsets()
    geometryDirty = false
    ringBitmapsDirty = true
  }

  private fun prepareHighlightRadiusScales(
    dotCount: Int,
    centerIndex: Int,
    push: Float
  ): FloatArray {
    if (highlightRadiusScales.size < dotCount) {
      highlightRadiusScales = FloatArray(dotCount)
    }
    for (index in 0 until dotCount) {
      highlightRadiusScales[index] = 1f
    }

    val center = wrapIndex(centerIndex, dotCount)
    highlightRadiusScales[center] = 1f + push
    if (dotCount > 1) {
      val left = wrapIndex(center - 1, dotCount)
      val right = wrapIndex(center + 1, dotCount)
      if (left != center) {
        highlightRadiusScales[left] = 1f + push * 0.55f
      }
      if (right != center && right != left) {
        highlightRadiusScales[right] = 1f + push * 0.55f
      }
    }
    return highlightRadiusScales
  }

  private fun ensureRingBitmaps(
    radii: FloatArray,
    phaseOffsets: FloatArray
  ) {
    val needsRebuild = ringBitmapsDirty || ringBitmaps.size != ringSpecs.size

    if (!needsRebuild) {
      return
    }

    clearRingBitmaps()
    if (width <= 0 || height <= 0) return

    val basePaint = paint
    val originalColor = basePaint.color
    val originalAlpha = basePaint.alpha
    basePaint.alpha = RING_ALPHA

    ringSpecs.forEachIndexed { index, spec ->
      if (spec.dotCount <= 0) {
        ringBitmaps.add(null)
        return@forEachIndexed
      }

      val radius = radii.getOrNull(index) ?: 0f
      val bitmapRadius = radius + spec.dotRadiusPx * 1.5f
      val bitmapSize = ceil(bitmapRadius * 2f).toInt().coerceAtLeast(1)
      val bitmapCenter = bitmapSize / 2f
      val bitmap = createBitmap(bitmapSize, bitmapSize)
      val bitmapCanvas = Canvas(bitmap)
      val phaseOffset = phaseOffsets.getOrNull(index) ?: 0f
      val angleStep = 360f / spec.dotCount

      for (dotIndex in 0 until spec.dotCount) {
        val baseAngle = dotIndex * angleStep
        val angleDeg = baseAngle + phaseOffset
        val angleRad = Math.toRadians(angleDeg.toDouble())
        val x = bitmapCenter + cos(angleRad) * radius
        val y = bitmapCenter + sin(angleRad) * radius

        val angleFraction = baseAngle / 360f
        basePaint.color = sampleGradient(normalizeUnit(angleFraction + spec.colorShift))
        bitmapCanvas.drawCircle(x.toFloat(), y.toFloat(), spec.dotRadiusPx, basePaint)
      }

      ringBitmaps.add(RingBitmap(bitmap))
    }

    ringBitmapsDirty = false
    basePaint.color = originalColor
    basePaint.alpha = originalAlpha
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
        invalidateOnAnimationFrame()
      }
    } else {
      invalidateOnAnimationFrame()
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

    paint.color = ColorUtils.setAlphaComponent(tint, (alpha * 0.24f).toInt())
    canvas.drawCircle(x, y, size * 1.22f, paint)

    paint.color = ColorUtils.setAlphaComponent(tint, (alpha * 0.62f).toInt())
    canvas.drawCircle(x, y, size, paint)

    bitmap?.let {
      val inset = size * 0.12f
      highlightDst.set(x - size + inset, y - size + inset, x + size - inset, y + size - inset)
      paint.alpha = (170 + 85 * progress).toInt().coerceIn(0, 255)
      canvas.drawBitmap(it, null, highlightDst, paint)
      paint.alpha = 255
    }

    paint.color = originalColor
    paint.alpha = originalAlpha
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
    clearHighlightBitmaps()
    if (bitmap == null) {
      invalidateOnAnimationFrame()
      return
    }

    highlightBitmapChannel.trySend(HighlightBitmap(bitmap, recyclable = false))
    notifyBitmapQueueAvailable()
  }

  fun setAppIconHighlightProvider(loadIcon: suspend () -> Drawable?) {
    setHighlightIconProvider(object : HighlightIconProvider {
      override suspend fun produce(emitter: HighlightIconEmitter) {
        val defaultIcon = context.packageManager.defaultActivityIcon
        var misses = 0
        while (true) {
          currentCoroutineContext().ensureActive()
          if (!isHighlightAnimationAvailable()) {
            break
          }
          val icon = loadIcon()
          if (icon == null) {
            delay(HIGHLIGHT_PROVIDER_RETRY_DELAY_MS)
            continue
          }
          val drawable = icon.takeIf { !UiUtils.drawablesAreEqual(it, defaultIcon) }
          if (drawable == null) {
            misses++
            if (misses >= HIGHLIGHT_PROVIDER_MAX_MISSES) {
              misses = 0
              delay(HIGHLIGHT_PROVIDER_RETRY_DELAY_MS)
            }
            continue
          }

          misses = 0
          emitter.emit(drawable.toCircularBitmap())
        }
      }
    })
  }

  fun setRuleIconHighlightProvider(
    withCircleBackground: Boolean = false,
    @ColorRes circleBackgroundColorRes: Int = R.color.feature_background
  ) {
    setHighlightIconProvider(object : HighlightIconProvider {
      override suspend fun produce(emitter: HighlightIconEmitter) {
        var misses = 0
        while (true) {
          currentCoroutineContext().ensureActive()
          if (!isHighlightAnimationAvailable()) {
            break
          }
          val index = Random.nextInt(RULE_ICON_POOL_COUNT)
          if (IconResMap.isSingleColorIcon(index)) {
            misses++
            if (misses >= HIGHLIGHT_PROVIDER_MAX_MISSES) {
              misses = 0
              delay(HIGHLIGHT_PROVIDER_RETRY_DELAY_MS)
            }
            continue
          }
          val drawable = ContextCompat.getDrawable(context, IconResMap.getIconRes(index))
          if (drawable == null) {
            misses++
            if (misses >= HIGHLIGHT_PROVIDER_MAX_MISSES) {
              misses = 0
              delay(HIGHLIGHT_PROVIDER_RETRY_DELAY_MS)
            }
            continue
          }

          misses = 0
          val icon = if (withCircleBackground) {
            UiUtils.addCircleBackground(context, drawable, circleBackgroundColorRes)
          } else {
            drawable
          }
          emitter.emit(icon.toBitmap())
        }
      }
    })
  }

  fun setHighlightIconProvider(provider: HighlightIconProvider?) {
    if (highlightIconProvider === provider) return

    stopHighlightAnimator()
    stopIconProducer()
    clearHighlightBitmaps()
    highlightIconProvider = provider
    updateHighlightAvailability()

    if (provider != null && isAttachedToWindow && isRunning) {
      startIconProducerJob(provider)
    }

    if (isAttachedToWindow && isRunning) {
      ensureHighlightAnimator()
    } else {
      invalidateOnAnimationFrame()
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
    clearQueuedHighlightBitmaps()
    updateHighlightAvailability()
  }

  private fun invalidateRingBitmaps() {
    geometryDirty = true
    ringBitmapsDirty = true
    clearRingBitmaps()
  }

  private fun clearRingBitmaps() {
    if (ringBitmaps.isEmpty()) return
    ringBitmaps.forEach { ringBitmap ->
      val bitmap = ringBitmap?.bitmap
      if (bitmap != null && !bitmap.isRecycled) {
        bitmap.recycle()
      }
    }
    ringBitmaps.clear()
  }

  private fun invalidateOnAnimationFrame() {
    if (isAttachedToWindow) {
      postInvalidateOnAnimation()
    } else {
      invalidate()
    }
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
        0.58f,
        count,
        context.dpToDimension(2.2f),
        rotationScale = 0.86f,
        colorShift = -0.02f,
        phaseOffsetMultiplier = 0f
      ),
      RingSpec(
        0.70f,
        count,
        context.dpToDimension(3.5f),
        rotationScale = 0.96f,
        colorShift = 0.04f,
        phaseOffsetMultiplier = 0f
      ),
      RingSpec(
        0.82f,
        count,
        context.dpToDimension(3.1f),
        rotationScale = 1.05f,
        colorShift = 0.08f,
        phaseOffsetMultiplier = 0f
      ),
      RingSpec(
        0.9f,
        count,
        context.dpToDimension(5.0f),
        rotationScale = 1.14f,
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
