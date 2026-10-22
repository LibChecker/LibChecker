package com.absinthe.libchecker.view.app

import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Rect
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import androidx.core.animation.doOnEnd
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.view.drawable.G2PillDrawable
import com.absinthe.libchecker.view.drawable.calculateFloatingNavSliderBounds
import com.google.android.material.R as MaterialR
import com.google.android.material.navigation.NavigationBarView
import kotlin.math.abs
import kotlin.math.roundToInt

// Keep circular end caps; smoothing changes their silhouette into a squircle.
internal const val FLOATING_NAV_CORNER_SMOOTHING = 0f

interface FloatingNavigationBar {
  var isFloating: Boolean
  val currentFloatingProgress: Float

  fun setFloatingProgress(progress: Float)

  fun setBlurProgress(progress: Float)

  fun suppressDragUntilRelease()

  fun setSelectedIndex(index: Int, animate: Boolean = true)
}

internal class FloatingNavigationThumbController(
  private val host: NavigationBarView,
  private val vertical: Boolean
) : FloatingNavigationBar {
  private val thumbView = View(host.context)
  private val thumbDrawable: G2PillDrawable
  private val touchSlop = ViewConfiguration.get(host.context).scaledTouchSlop
  private val originalIndicatorColor: ColorStateList? = host.itemActiveIndicatorColor
  private val originalRippleColor = host.itemRippleColor
  private var downX = 0f
  private var downY = 0f
  private var dragging = false
  private var suppressDrag = false
  private val isThumbAnimating: Boolean
    get() = thumbPositionAnimator != null
  private var pointerPressed = false
  private var thumbTargetIndex = 0
  private var thumbPositionAnimator: ValueAnimator? = null
  private var thumbScaleAnimator: ValueAnimator? = null
  private var thumbTargetScale = 1f
  private var pendingIndex = 0
  private var floatingProgress = 0f
  private var blurProgress = 0f
  private var normalIndicatorLeft = Float.NaN
  private var normalIndicatorTop = Float.NaN
  private var normalIndicatorWidth = 0f
  private var normalIndicatorHeight = 0f
  private var normalItemStride = 0f

  override var isFloating: Boolean = false

  override val currentFloatingProgress: Float
    get() = floatingProgress

  private var selectedIndex = 0

  init {
    host.clipChildren = false
    host.clipToPadding = false
    val thumbColor = host.context.getColorByAttr(MaterialR.attr.colorSecondaryContainer)
    thumbDrawable = G2PillDrawable(fillColor = thumbColor, cornerSmoothing = if (vertical) null else FLOATING_NAV_CORNER_SMOOTHING)
    thumbView.background = thumbDrawable
    thumbView.visibility = View.GONE
    host.addView(thumbView, 0, FrameLayout.LayoutParams(0, 0))
  }

  override fun setFloatingProgress(progress: Float) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    floatingProgress = clampedProgress
    updateThumbAlpha()
    if (clampedProgress > 0f) {
      if (thumbView.visibility != View.VISIBLE) {
        thumbView.visibility = View.VISIBLE
        host.itemActiveIndicatorColor = originalIndicatorColor?.withAlpha(0)
        host.itemRippleColor = null
      }
    } else {
      cancelThumbAnimation()
      pointerPressed = false
      releaseThumbScale(animate = false)
      if (thumbView.visibility != View.GONE) {
        thumbView.visibility = View.GONE
        host.itemActiveIndicatorColor = originalIndicatorColor
        host.itemRippleColor = originalRippleColor
      }
    }
  }

  override fun setBlurProgress(progress: Float) {
    blurProgress = progress.coerceIn(0f, 1f)
    updateThumbAlpha()
  }

  private fun updateThumbAlpha() {
    thumbDrawable.setAlpha((255f - 55f * blurProgress * floatingProgress).roundToInt())
  }

  override fun suppressDragUntilRelease() {
    dragging = false
    suppressDrag = true
    host.parent?.requestDisallowInterceptTouchEvent(false)
  }

  fun onDispatchTouchEvent(event: MotionEvent) {
    if (!isFloating) return
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        suppressDrag = false
        pointerPressed = true
        animateThumbScale(THUMB_PRESSED_SCALE)
        val bounds = Rect()
        for (index in 0 until host.menu.size()) {
          val item = host.findViewById<View>(host.menu.getItem(index).itemId) ?: continue
          bounds.set(0, 0, item.width, item.height)
          host.offsetDescendantRectToMyCoords(item, bounds)
          if (bounds.contains(event.x.toInt(), event.y.toInt())) {
            animateThumbTo(index)
            break
          }
        }
      }

      MotionEvent.ACTION_UP -> {
        pointerPressed = false
        suppressDrag = false
        if (!dragging && !isTouchInsideAnyItem(event)) {
          animateThumbTo(selectedIndex)
        }
        releaseThumbScale()
      }

      MotionEvent.ACTION_CANCEL -> {
        pointerPressed = false
        suppressDrag = false
        if (!dragging) animateThumbTo(selectedIndex)
        releaseThumbScale()
      }
    }
  }

  fun onMeasure() {
    val (containerWidth, containerHeight) = containerSize()
    if (containerWidth <= 0f || containerHeight <= 0f) return
    val bounds = sliderBounds(containerWidth, containerHeight)
    thumbView.measure(
      View.MeasureSpec.makeMeasureSpec((bounds.right - bounds.left).roundToInt().coerceAtLeast(0), View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec((bounds.bottom - bounds.top).roundToInt().coerceAtLeast(0), View.MeasureSpec.EXACTLY)
    )
  }

  fun onLayout() {
    if (floatingProgress == 0f) captureNormalIndicatorGeometry()
    if (!isFloating && thumbView.visibility != View.VISIBLE) return
    val (containerWidth, containerHeight) = containerSize()
    if (containerWidth <= 0f || containerHeight <= 0f) return

    val bounds = sliderBounds(containerWidth, containerHeight)
    val thumbLeft = bounds.left.roundToInt()
    val thumbTop = bounds.top.roundToInt()
    thumbView.layout(
      thumbLeft,
      thumbTop,
      thumbLeft + thumbView.measuredWidth,
      thumbTop + thumbView.measuredHeight
    )
    if (!dragging) {
      val position = thumbPositionAnimator?.animatedValue as? Float ?: selectedIndex.toFloat()
      setThumbTranslation(position * currentItemStride(containerWidth, containerHeight))
    }
  }

  fun onInterceptTouchEvent(event: MotionEvent): Boolean? {
    if (!isFloating || suppressDrag) return null
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> prepareGesture(event)

      MotionEvent.ACTION_MOVE -> {
        if (isDragGesture(event)) {
          dragging = true
          host.parent?.requestDisallowInterceptTouchEvent(true)
          updateDrag(pointerPosition(event), segmentExtent())
          return true
        }
      }
    }
    return null
  }

  fun onTouchEvent(event: MotionEvent): Boolean? {
    if (!isFloating || suppressDrag) return null
    val segmentExtent = segmentExtent()
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        prepareGesture(event)
        return true
      }

      MotionEvent.ACTION_MOVE -> {
        if (!dragging && isDragGesture(event)) {
          dragging = true
          host.parent?.requestDisallowInterceptTouchEvent(true)
        }
        if (dragging) {
          updateDrag(pointerPosition(event), segmentExtent)
          return true
        }
      }

      MotionEvent.ACTION_UP -> {
        if (dragging) {
          updateDrag(pointerPosition(event), segmentExtent)
          finishDrag(commit = true)
          return true
        }
      }

      MotionEvent.ACTION_CANCEL -> {
        if (dragging) {
          finishDrag(commit = false)
          return true
        }
      }
    }
    return null
  }

  override fun setSelectedIndex(index: Int, animate: Boolean) {
    val targetIndex = index.coerceIn(0, (host.menu.size() - 1).coerceAtLeast(0))
    selectedIndex = targetIndex
    if (dragging) return

    val (containerWidth, containerHeight) = containerSize()
    val targetTranslation = targetIndex * currentItemStride(containerWidth, containerHeight)
    if (animate && host.isAttachedToWindow && segmentExtent() > 0f) {
      animateThumbTo(targetIndex)
    } else {
      cancelThumbAnimation()
      setThumbTranslation(targetTranslation)
    }
  }

  fun onDetachedFromWindow() {
    cancelThumbAnimation()
    pointerPressed = false
    thumbScaleAnimator?.cancel()
    thumbScaleAnimator = null
    releaseThumbScale(animate = false)
    dragging = false
    suppressDrag = false
    host.parent?.requestDisallowInterceptTouchEvent(false)
  }

  private fun prepareGesture(event: MotionEvent) {
    downX = event.x
    downY = event.y
    dragging = false
    pendingIndex = selectedIndex
  }

  private fun isDragGesture(event: MotionEvent): Boolean {
    val dx = abs(event.x - downX)
    val dy = abs(event.y - downY)
    return if (vertical) {
      dy > touchSlop && dy > dx
    } else {
      dx > touchSlop && dx > dy
    }
  }

  private fun pointerPosition(event: MotionEvent): Float {
    if (vertical) return event.y
    return if (host.layoutDirection == View.LAYOUT_DIRECTION_RTL) host.width - event.x else event.x
  }

  private fun updateDrag(pointer: Float, segmentExtent: Float) {
    cancelThumbAnimation()
    val maxIndex = (host.menu.size() - 1).coerceAtLeast(0)
    val rawTranslation = if (vertical) {
      pointer - host.width / 2f
    } else {
      val spacing = SLIDER_INSET_DP * host.resources.displayMetrics.density
      val thumbExtent = (segmentExtent - 2f * spacing).coerceAtLeast(0f)
      pointer - spacing - thumbExtent / 2f
    }
    val translation = rawTranslation.coerceIn(0f, maxIndex * segmentExtent)
    setThumbTranslation(translation)

    val nextIndex = (translation / segmentExtent).roundToInt().coerceIn(0, maxIndex)
    if (nextIndex != pendingIndex) {
      pendingIndex = nextIndex
      host.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
  }

  private fun finishDrag(commit: Boolean) {
    dragging = false
    host.parent?.requestDisallowInterceptTouchEvent(false)
    val targetIndex = if (commit) pendingIndex else selectedIndex
    animateThumbTo(targetIndex)

    if (commit && targetIndex != selectedIndex && targetIndex in 0 until host.menu.size()) {
      selectedIndex = targetIndex
      host.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
      host.selectedItemId = host.menu.getItem(targetIndex).itemId
    }
  }

  private fun animateThumbTo(targetIndex: Int) {
    if (isThumbAnimating && thumbTargetIndex == targetIndex) return
    cancelThumbAnimation()
    thumbTargetIndex = targetIndex
    val stride = segmentExtent()
    val targetTranslation = targetIndex * stride
    val currentTranslation = if (vertical) thumbView.translationY else thumbView.translationX * horizontalDirection()
    if (stride <= 0f || abs(currentTranslation - targetTranslation) < 0.5f) {
      if (!pointerPressed) releaseThumbScale()
      return
    }
    val delta = abs(targetIndex - currentTranslation / stride).coerceAtLeast(1f)
    val tension = (BASE_OVERSHOOT_TENSION / delta).coerceIn(0.35f, BASE_OVERSHOOT_TENSION)
    // IME resizing changes the stride while the selection animation is running.
    thumbPositionAnimator = ValueAnimator.ofFloat(currentTranslation / stride, targetIndex.toFloat()).apply {
      duration = THUMB_ANIMATION_DURATION_MS
      interpolator = OvershootInterpolator(tension)
      addUpdateListener {
        setThumbTranslation((it.animatedValue as Float) * segmentExtent())
      }
      doOnEnd {
        thumbPositionAnimator = null
        if (!pointerPressed) releaseThumbScale()
      }
    }
    thumbPositionAnimator?.start()
  }

  private fun isTouchInsideAnyItem(event: MotionEvent): Boolean {
    val bounds = Rect()
    for (index in 0 until host.menu.size()) {
      val item = host.findViewById<View>(host.menu.getItem(index).itemId) ?: continue
      bounds.set(0, 0, item.width, item.height)
      host.offsetDescendantRectToMyCoords(item, bounds)
      if (bounds.contains(event.x.toInt(), event.y.toInt())) {
        return true
      }
    }
    return false
  }

  private fun setThumbTranslation(translation: Float) {
    val stride = segmentExtent()
    val maxIndex = (host.menu.size() - 1).coerceAtLeast(0)
    val maxOvershoot = SLIDER_INSET_DP * host.resources.displayMetrics.density * 0.5f
    val clampedTranslation = translation.coerceIn(-maxOvershoot, maxIndex * stride + maxOvershoot)
    if (vertical) {
      thumbView.translationY = clampedTranslation
    } else {
      thumbView.translationX = horizontalDirection() * clampedTranslation
    }
  }

  private fun horizontalDirection(): Float = if (host.layoutDirection == View.LAYOUT_DIRECTION_RTL) -1f else 1f

  private fun currentItemStride(containerWidth: Float, containerHeight: Float): Float {
    val count = host.menu.size().coerceAtLeast(1)
    val targetStride = when {
      !vertical -> containerWidth / count
      count == 1 -> 0f
      else -> (containerHeight - containerWidth).coerceAtLeast(0f) / (count - 1)
    }
    val startStride = normalItemStride.takeIf { it > 0f } ?: targetStride
    return startStride + (targetStride - startStride) * floatingProgress
  }

  private fun segmentExtent(): Float {
    return currentItemStride(host.width.toFloat(), host.height.toFloat())
  }

  private fun sliderBounds(containerWidth: Float, containerHeight: Float) = calculateFloatingNavSliderBounds(
    containerWidth = containerWidth,
    containerHeight = containerHeight,
    position = if (!vertical && host.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
      (host.menu.size() - 1).coerceAtLeast(0).toFloat()
    } else {
      0f
    },
    spacing = SLIDER_INSET_DP * host.resources.displayMetrics.density,
    itemCount = host.menu.size(),
    progress = floatingProgress,
    normalWidth = resolvedNormalIndicatorWidth(),
    normalHeight = resolvedNormalIndicatorHeight(),
    normalTop = resolvedNormalIndicatorTop(containerHeight),
    normalLeft = resolvedNormalIndicatorLeft(containerWidth),
    vertical = vertical
  )

  private fun captureNormalIndicatorGeometry() {
    if (host.menu.size() == 0) return
    val firstItemView = host.findViewById<View>(host.menu.getItem(0).itemId) ?: return
    val firstIndicator = firstItemView.findViewById<View>(MaterialR.id.navigation_bar_item_active_indicator_view) ?: return
    val selectedItemView = host.findViewById<View>(host.selectedItemId) ?: return
    val selectedIndicator = selectedItemView.findViewById<View>(MaterialR.id.navigation_bar_item_active_indicator_view) ?: return
    if (firstIndicator.width <= 0 || firstIndicator.height <= 0 || selectedIndicator.width <= 0 || selectedIndicator.height <= 0) return
    val firstIndicatorBounds = Rect(0, 0, firstIndicator.width, firstIndicator.height)
    val selectedIndicatorBounds = Rect(0, 0, selectedIndicator.width, selectedIndicator.height)
    host.offsetDescendantRectToMyCoords(firstIndicator, firstIndicatorBounds)
    host.offsetDescendantRectToMyCoords(selectedIndicator, selectedIndicatorBounds)
    normalIndicatorLeft = if (vertical) selectedIndicatorBounds.left.toFloat() else firstIndicatorBounds.left.toFloat()
    normalIndicatorTop = if (vertical) firstIndicatorBounds.top.toFloat() else selectedIndicatorBounds.top.toFloat()
    normalIndicatorWidth = selectedIndicatorBounds.width().toFloat()
    normalIndicatorHeight = selectedIndicatorBounds.height().toFloat()

    if (host.menu.size() > 1) {
      val secondItemView = host.findViewById<View>(host.menu.getItem(1).itemId) ?: return
      val firstBounds = Rect(0, 0, firstItemView.width, firstItemView.height)
      val secondBounds = Rect(0, 0, secondItemView.width, secondItemView.height)
      host.offsetDescendantRectToMyCoords(firstItemView, firstBounds)
      host.offsetDescendantRectToMyCoords(secondItemView, secondBounds)
      normalItemStride = if (vertical) {
        abs(secondBounds.exactCenterY() - firstBounds.exactCenterY())
      } else {
        abs(secondBounds.exactCenterX() - firstBounds.exactCenterX())
      }
    }
  }

  private fun resolvedNormalIndicatorWidth(): Float = normalIndicatorWidth.takeIf { it > 0f } ?: host.itemActiveIndicatorWidth.toFloat()

  private fun resolvedNormalIndicatorHeight(): Float = normalIndicatorHeight.takeIf { it > 0f } ?: host.itemActiveIndicatorHeight.toFloat()

  private fun resolvedNormalIndicatorLeft(containerWidth: Float): Float = normalIndicatorLeft.takeUnless { it.isNaN() } ?: (containerWidth - resolvedNormalIndicatorWidth()) / 2f

  private fun resolvedNormalIndicatorTop(containerHeight: Float): Float = normalIndicatorTop.takeUnless { it.isNaN() } ?: (containerHeight - resolvedNormalIndicatorHeight()) / 2f

  private fun containerSize(): Pair<Float, Float> {
    val height = if (vertical) {
      host.measuredHeight
    } else {
      (host.measuredHeight - host.paddingBottom).coerceAtLeast(0)
    }
    return host.measuredWidth.toFloat() to height.toFloat()
  }

  private fun cancelThumbAnimation() {
    thumbPositionAnimator?.removeAllListeners()
    thumbPositionAnimator?.cancel()
    thumbPositionAnimator = null
  }

  private fun animateThumbScale(target: Float, animate: Boolean = true) {
    if (thumbTargetScale == target && thumbScaleAnimator?.isRunning == true) return
    if (!animate && thumbView.scaleX == target && thumbView.scaleY == target) return
    thumbTargetScale = target
    thumbScaleAnimator?.cancel()
    if (!animate) {
      thumbView.scaleX = target
      thumbView.scaleY = target
      thumbScaleAnimator = null
      return
    }
    thumbScaleAnimator = ValueAnimator.ofFloat(thumbView.scaleX, target).apply {
      duration = if (target < 1f) THUMB_PRESS_DURATION_MS else THUMB_RELEASE_DURATION_MS
      interpolator = if (target < 1f) THUMB_PRESS_INTERPOLATOR else THUMB_RELEASE_INTERPOLATOR
      addUpdateListener {
        val scale = it.animatedValue as Float
        thumbView.scaleX = scale
        thumbView.scaleY = scale
      }
      doOnEnd {
        thumbScaleAnimator = null
      }
      start()
    }
  }

  private fun releaseThumbScale(animate: Boolean = true) {
    if (thumbView.scaleX == 1f && thumbView.scaleY == 1f && thumbScaleAnimator == null) return
    if (thumbScaleAnimator?.isRunning == true && thumbTargetScale == 1f) return
    animateThumbScale(1f, animate)
  }

  private companion object {
    const val SLIDER_INSET_DP = 4f
    const val THUMB_ANIMATION_DURATION_MS = 280L
    const val THUMB_PRESS_DURATION_MS = 180L
    const val THUMB_RELEASE_DURATION_MS = 280L
    const val THUMB_PRESSED_SCALE = 0.9f
    const val BASE_OVERSHOOT_TENSION = 1.15f
    val THUMB_PRESS_INTERPOLATOR = PathInterpolator(0.2f, 0f, 0f, 1f)
    val THUMB_RELEASE_INTERPOLATOR = OvershootInterpolator(1.15f)
  }
}
