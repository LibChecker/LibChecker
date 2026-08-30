package com.absinthe.libchecker.domain.home.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.os.SystemClock
import android.text.TextUtils
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.PathInterpolator
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.dpToDimension
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import kotlin.math.abs
import kotlin.math.roundToInt

internal class RecentVisitRow(context: Context, item: RecentVisitItem) : LinearLayout(context) {
  private val icon = RemovalIconView(context).apply {
    setImageDrawable(item.icon.constantState?.newDrawable(resources)?.mutate() ?: item.icon)
    if (item.tintIcon) {
      imageTintList = ColorStateList.valueOf(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
    }
    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
  }
  private var onRemove: (() -> Unit)? = null
  private var onHoldDrag: ((PointF) -> Boolean)? = null
  private var touching = false
  private var holding = false
  private var consumedHold = false
  private var removed = false
  private var downX = 0f
  private var downY = 0f
  private var lastHoldTime = 0L
  private var retreatAnimator: ValueAnimator? = null
  private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
  private val advanceHold = object : Runnable {
    override fun run() {
      if (!holding || !touching || !isAttachedToWindow) return
      val now = SystemClock.uptimeMillis()
      // Confirmation follows actual held time, even when system animations are disabled.
      icon.removalProgress = (icon.removalProgress + (now - lastHoldTime) / 1200f).coerceAtMost(1f)
      lastHoldTime = now
      if (icon.removalProgress == 1f) {
        holding = false
        removed = true
        performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        onRemove?.invoke()
      } else {
        postOnAnimation(this)
      }
    }
  }

  init {
    gravity = Gravity.CENTER_VERTICAL
    minimumHeight = dp(52)
    setPaddingRelative(dp(16), dp(10), dp(16), dp(10))
    background = RippleDrawable(
      ColorStateList.valueOf(context.getColorByAttr(android.R.attr.colorControlHighlight)),
      null,
      ColorDrawable(Color.WHITE)
    )
    addView(icon, LayoutParams(dp(32), dp(32)))
    addView(
      AppCompatTextView(context).apply {
        text = item.label
        textSize = 15f
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
        includeFontPadding = false
        setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
      },
      LayoutParams(0, -2, 1f).apply { marginStart = dp(14) }
    )
    contentDescription = item.label
    isFocusable = true
  }

  fun setOnHoldToRemove(onDrag: ((PointF) -> Boolean)? = null, action: () -> Unit) {
    onRemove = action
    onHoldDrag = onDrag
    setOnLongClickListener {
      if (!removed) {
        if (touching) {
          retreatAnimator?.cancel()
          holding = true
          consumedHold = true
          isPressed = false
          parent?.requestDisallowInterceptTouchEvent(true)
          lastHoldTime = SystemClock.uptimeMillis()
          postOnAnimation(advanceHold)
        } else {
          // Keyboard long-click; TalkBack also has an explicitly labelled delete action.
          action()
        }
      }
      true
    }
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    if (onRemove == null) return super.onTouchEvent(event)
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        touching = true
        consumedHold = false
        downX = event.x
        downY = event.y
      }

      MotionEvent.ACTION_MOVE -> if (abs(event.x - downX) > touchSlop || abs(event.y - downY) > touchSlop) {
        val drag = onHoldDrag.takeIf { holding && !removed }
        consumedHold = true
        cancelLongPress()
        releaseHold()
        if (drag?.invoke(PointF(event.x, event.y)) == true) {
          retreatAnimator?.cancel()
          icon.removalProgress = 0f
        }
      }

      MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> {
        releaseHold()
        if (consumedHold || removed || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
          val cancel = MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
          super.onTouchEvent(cancel)
          cancel.recycle()
          return true
        }
      }
    }
    return super.onTouchEvent(event)
  }

  private fun releaseHold() {
    touching = false
    holding = false
    removeCallbacks(advanceHold)
    parent?.requestDisallowInterceptTouchEvent(false)
    if (removed || icon.removalProgress == 0f || retreatAnimator?.isRunning == true) return
    retreatAnimator = ValueAnimator.ofFloat(icon.removalProgress, 0f).apply {
      duration = 360L
      interpolator = PathInterpolator(.2f, 0f, 0f, 1f)
      addUpdateListener { icon.removalProgress = it.animatedValue as Float }
      start()
    }
  }

  override fun onDetachedFromWindow() {
    touching = false
    holding = false
    removeCallbacks(advanceHold)
    cancelLongPress()
    parent?.requestDisallowInterceptTouchEvent(false)
    retreatAnimator?.cancel()
    retreatAnimator = null
    icon.removalProgress = 0f
    super.onDetachedFromWindow()
  }

  private fun dp(value: Int) = context.dpToDimension(value.toFloat()).roundToInt()

  private class RemovalIconView(context: Context) : AppCompatImageView(context) {
    private val removalColor = context.getColorByAttr(androidx.appcompat.R.attr.colorError)
    private val removalIcon by lazy {
      ContextCompat.getDrawable(context, R.drawable.ic_delete_outline)?.mutate()?.apply { setTint(removalColor) }
    }
    private val ring = RectF()
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = removalColor
      style = Paint.Style.STROKE
      strokeWidth = context.dpToDimension(1.75f)
      strokeCap = Paint.Cap.ROUND
    }
    var removalProgress = 0f
      set(value) {
        field = value
        imageAlpha = (255f * (1f - morphFraction())).roundToInt()
        invalidate()
      }

    private fun morphFraction() = (removalProgress / .16f).coerceIn(0f, 1f)

    override fun onDraw(canvas: Canvas) {
      val morph = morphFraction()
      if (morph == 0f) {
        super.onDraw(canvas)
        return
      }
      val centerX = width / 2f
      val centerY = height / 2f
      val checkpoint = canvas.save()
      canvas.scale(1f - .2f * morph, 1f - .2f * morph, centerX, centerY)
      super.onDraw(canvas)
      canvas.restoreToCount(checkpoint)
      val radius = minOf(width, height) / 2f - ringPaint.strokeWidth
      ring.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
      ringPaint.alpha = (40f * morph).roundToInt()
      canvas.drawOval(ring, ringPaint)
      ringPaint.alpha = (255f * morph).roundToInt()
      canvas.drawArc(ring, -90f, 360f * removalProgress, false, ringPaint)
      removalIcon?.let {
        val halfSize = minOf(width, height) * .34f * (.8f + .2f * morph)
        it.setBounds((centerX - halfSize).roundToInt(), (centerY - halfSize).roundToInt(), (centerX + halfSize).roundToInt(), (centerY + halfSize).roundToInt())
        it.alpha = (255f * morph).roundToInt()
        it.draw(canvas)
      }
    }
  }
}
