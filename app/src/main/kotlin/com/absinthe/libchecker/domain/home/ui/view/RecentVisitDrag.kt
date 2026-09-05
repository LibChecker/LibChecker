package com.absinthe.libchecker.domain.home.ui.view

import android.animation.ValueAnimator
import android.content.ClipData
import android.content.ClipDescription
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import android.view.DragEvent
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.PathInterpolator
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.children
import androidx.core.view.drawToBitmap
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.dpToDimension
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.view.drawable.setG2Shape
import kotlin.math.abs
import kotlin.math.roundToInt

internal const val SHORTCUT_DRAG_MIME = "application/vnd.libchecker.shortcut"

/** Observe the gesture without taking ordinary taps or vertical/horizontal scrolling. */
fun installRecentVisitDrag(list: RecyclerView, onHold: (View, Int, PointF?) -> Boolean) {
  val slop = ViewConfiguration.get(list.context).scaledTouchSlop
  var touched: View? = null
  val down = PointF()
  val touch = PointF()
  val hold = Runnable {
    val row = touched
    touched = null
    if (row != null && row.isAttachedToWindow && list.scrollState == RecyclerView.SCROLL_STATE_IDLE) {
      val position = list.getChildAdapterPosition(row)
      if (position != RecyclerView.NO_POSITION) onHold(row, position, PointF(touch.x, touch.y))
    }
  }
  fun cancel() {
    list.removeCallbacks(hold)
    touched = null
  }
  list.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
    override fun onInterceptTouchEvent(rv: RecyclerView, event: MotionEvent): Boolean {
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          cancel()
          touched = rv.findChildViewUnder(event.x, event.y)
          down.set(event.x, event.y)
          touched?.let {
            touch.set(event.x - it.x, event.y - it.y)
            list.postDelayed(hold, maxOf(750L, ViewConfiguration.getLongPressTimeout().toLong() + 200L))
          }
        }

        MotionEvent.ACTION_MOVE -> if (abs(event.x - down.x) > slop || abs(event.y - down.y) > slop) cancel()

        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> cancel()
      }
      return false
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
      if (disallowIntercept) cancel()
    }
  })
  fun bindAccessibility(row: View) {
    ViewCompat.replaceAccessibilityAction(
      row,
      AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_LONG_CLICK,
      list.context.getString(R.string.shortcuts_pin)
    ) { _, _ ->
      val position = list.getChildAdapterPosition(row)
      position != RecyclerView.NO_POSITION && onHold(row, position, null)
    }
  }
  list.children.forEach(::bindAccessibility)
  list.addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
    override fun onChildViewAttachedToWindow(view: View) = bindAccessibility(view)
    override fun onChildViewDetachedFromWindow(view: View) {
      if (view === touched) cancel()
    }
  })
  list.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
    override fun onViewAttachedToWindow(view: View) = Unit
    override fun onViewDetachedFromWindow(view: View) = cancel()
  })
}

/** The platform owns pointer delivery across the activity and PopupWindow. */
fun startRecentVisitDrag(
  source: View,
  item: RecentVisitItem,
  token: String,
  onEnd: (Boolean) -> Unit,
  onLocation: ((Float, Float) -> Unit)? = null
): Boolean {
  val shadow = object : View.DragShadowBuilder() {
    override fun onProvideShadowMetrics(size: Point, hotspot: Point) {
      size.set(1, 1)
      hotspot.set(0, 0)
    }

    override fun onDrawShadow(canvas: Canvas) = Unit
  }
  val originalAlpha = source.alpha
  val detachListener = object : View.OnAttachStateChangeListener {
    override fun onViewAttachedToWindow(view: View) = Unit
    override fun onViewDetachedFromWindow(view: View) {
      source.cancelDragAndDrop()
      source.alpha = originalAlpha
      source.setOnDragListener(null)
      source.removeOnAttachStateChangeListener(this)
      onEnd(false)
    }
  }
  source.setOnDragListener { _, event ->
    if (event.action == DragEvent.ACTION_DRAG_LOCATION) onLocation?.invoke(event.x, event.y)
    if (event.action == DragEvent.ACTION_DRAG_ENDED) {
      source.alpha = originalAlpha
      source.setOnDragListener(null)
      source.removeOnAttachStateChangeListener(detachListener)
      onEnd(event.result)
    }
    event.localState === item && event.action != DragEvent.ACTION_DROP
  }
  // PopupWindow has its own window token, so it needs cross-window delivery. Only an
  // opaque session token leaves this window; the destination and icon stay in memory.
  val clip = ClipData(ClipDescription(token, arrayOf(SHORTCUT_DRAG_MIME)), ClipData.Item(""))
  val started = source.startDragAndDrop(clip, shadow, item, View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_OPAQUE)
  if (started) {
    source.addOnAttachStateChangeListener(detachListener)
    source.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    source.isPressed = false
    source.alpha = .25f
  } else {
    source.setOnDragListener(null)
  }
  return started
}

internal class RecentVisitDragPreview(
  private val source: View,
  private val touch: PointF,
  item: RecentVisitItem
) : View(source.context) {
  private val density = source.resources.displayMetrics.density
  private val compact = RecentVisitRow(source.context, item).apply {
    layoutDirection = source.layoutDirection
    measure(
      View.MeasureSpec.makeMeasureSpec(source.context.dpToDimension(264f).roundToInt(), View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    )
    layout(0, 0, measuredWidth, measuredHeight)
  }.drawToBitmap()
  private val original = runCatching { source.drawToBitmap() }.getOrNull()
  private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
  private val surfaceColor = source.context.getColorByAttr(com.google.android.material.R.attr.colorSurfaceContainerHigh)
  private val path = Path()
  private val bounds = RectF()
  private val targetLeft = (source.width - compact.width) * (touch.x / source.width).coerceIn(0f, 1f)
  private val targetTop = (source.height - compact.height) * (touch.y / source.height).coerceIn(0f, 1f)
  private val offsetX = maxOf(0f, -targetLeft)
  private val offsetY = maxOf(0f, -targetTop)
  private var fraction = 0f
  private var animator: ValueAnimator? = null

  init {
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    elevation = source.context.dpToDimension(16f)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    setMeasuredDimension(maxOf(source.width, compact.width), maxOf(source.height, compact.height))
  }

  fun moveTo(x: Float, y: Float) {
    translationX = x - touch.x - offsetX
    translationY = y - touch.y - offsetY
  }

  override fun onDraw(canvas: Canvas) {
    val save = canvas.save()
    canvas.translate(offsetX, offsetY)
    val p = fraction
    val left = targetLeft * p
    val top = targetTop * p
    bounds.set(left, top, left + source.width + (compact.width - source.width) * p, top + source.height + (compact.height - source.height) * p)
    path.setG2Shape(bounds.left, bounds.top, bounds.right, bounds.bottom, 18f * density * p)
    paint.color = surfaceColor
    paint.alpha = (255 * p).roundToInt()
    canvas.drawPath(path, paint)
    canvas.clipPath(path)
    original?.let {
      paint.alpha = (255 * (1f - p)).roundToInt()
      canvas.drawBitmap(it, null, bounds, paint)
    }
    paint.alpha = (255 * p).roundToInt()
    canvas.drawBitmap(compact, null, bounds, paint)
    canvas.restoreToCount(save)
  }

  fun start() {
    animator?.cancel()
    animator = ValueAnimator.ofFloat(fraction, 1f).apply {
      duration = 200L
      interpolator = PathInterpolator(.2f, 0f, 0f, 1f)
      addUpdateListener {
        fraction = it.animatedValue as Float
        invalidate()
      }
      start()
    }
  }

  fun stop() {
    animate().cancel()
    animator?.cancel()
    animator = null
    // The display list can still reference these bitmaps until the next render pass.
  }
}
