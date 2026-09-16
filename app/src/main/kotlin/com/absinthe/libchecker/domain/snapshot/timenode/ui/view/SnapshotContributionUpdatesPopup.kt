package com.absinthe.libchecker.domain.snapshot.timenode.ui.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.doOnLayout
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import com.absinthe.libchecker.domain.snapshot.timenode.model.DayContribution
import com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotUpdatedAppItem
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.extensions.dpToDimension
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.view.drawable.setG2Shape
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

class SnapshotContributionUpdatesPopup(
  private val host: View,
  private val anchorRect: Rect,
  private val dayContribution: DayContribution,
  private val packageIconSources: Map<String, SnapshotPackageIconSource>,
  private val onItemClick: (SnapshotUpdatedAppItem) -> Unit
) : PopupWindow(host.context) {

  private val context = host.context
  private val overlay = FrameLayout(context)
  private val surface = BalloonView(context)
  private val rows = LinearLayout(context).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(0, dp(6), 0, dp(6))
  }
  private var animator: ValueAnimator? = null
  private var progress = 0f
  private var closing = false

  init {
    contentView = overlay
    width = ViewGroup.LayoutParams.MATCH_PARENT
    height = ViewGroup.LayoutParams.MATCH_PARENT
    isFocusable = true
    isClippingEnabled = false
    inputMethodMode = INPUT_METHOD_NOT_NEEDED
    animationStyle = 0
    setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

    overlay.addView(surface, FrameLayout.LayoutParams(dp(270), -2))
    surface.addView(
      MaxHeightScrollView(context, dp(320)).apply {
        isFillViewport = false
        isVerticalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        addView(rows, ViewGroup.LayoutParams(-1, -2))
      },
      FrameLayout.LayoutParams(-1, -2)
    )

    surface.isClickable = true
    overlay.setOnClickListener { dismiss() }
    overlay.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
      if (oldRight > oldLeft && oldBottom > oldTop &&
        (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop)
      ) {
        dismissImmediately()
      }
    }

    populateContent()
    applyProgress(0f)
  }

  private fun populateContent() {
    rows.removeAllViews()
    rows.addView(buildHeader(dayContribution))

    val apps = dayContribution.updatedApps
    if (apps.isEmpty()) {
      rows.addView(buildEmptyMessage())
    } else {
      apps.forEach { app ->
        rows.addView(buildAppRow(app))
      }
    }
  }

  private fun buildHeader(day: DayContribution): View {
    val header = LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      setPadding(dp(16), dp(10), dp(16), dp(6))
    }
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val dateStr = day.date.format(formatter)

    val title = AppCompatTextView(context).apply {
      text = dateStr
      textSize = 14f
      setTypeface(typeface, android.graphics.Typeface.BOLD)
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
      includeFontPadding = false
      ViewCompat.setAccessibilityHeading(this, true)
    }
    header.addView(title)

    val subtitleText = buildString {
      if (day.isSnapshotDay) {
        append(context.getString(R.string.snapshot_contribution_snapshot_captured))
        append(" · ")
      }
      if (day.updateCount > 0) {
        append(
          context.resources.getQuantityString(
            R.plurals.snapshot_contribution_updates_count,
            day.updateCount,
            day.updateCount
          )
        )
      } else {
        append(context.getString(R.string.snapshot_contribution_no_updates))
      }
    }
    val subtitle = AppCompatTextView(context).apply {
      text = subtitleText
      textSize = 12f
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
      includeFontPadding = false
      setPadding(0, dp(2), 0, 0)
    }
    header.addView(subtitle)
    return header
  }

  private fun buildAppRow(item: SnapshotUpdatedAppItem): View {
    val row = LinearLayout(context).apply {
      orientation = LinearLayout.HORIZONTAL
      gravity = Gravity.CENTER_VERTICAL
      minimumHeight = dp(52)
      setPaddingRelative(dp(16), dp(8), dp(16), dp(8))
      background = RippleDrawable(
        ColorStateList.valueOf(context.getColorByAttr(android.R.attr.colorControlHighlight)),
        null,
        ColorDrawable(Color.WHITE)
      )
      isFocusable = true
      isClickable = true
      contentDescription = "${item.label}, ${item.packageName}"
      setOnClickListener {
        close { onItemClick(item) }
      }
    }

    val icon = AppCompatImageView(context).apply {
      val source = packageIconSources[item.packageName]
      load((source as? SnapshotPackageIconSource.InstalledPackage)?.packageInfo ?: R.drawable.ic_icon_blueprint) {
        placeholder(R.drawable.ic_icon_blueprint)
        error(R.drawable.ic_icon_blueprint)
        crossfade(false)
      }
      importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    row.addView(icon, LinearLayout.LayoutParams(dp(36), dp(36)))

    val textContainer = LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    val labelView = AppCompatTextView(context).apply {
      text = item.label
      textSize = 14f
      maxLines = 1
      ellipsize = TextUtils.TruncateAt.END
      includeFontPadding = false
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
    }
    textContainer.addView(labelView)

    val pkgView = AppCompatTextView(context).apply {
      text = item.packageName
      textSize = 11f
      maxLines = 1
      ellipsize = TextUtils.TruncateAt.END
      includeFontPadding = false
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
      setPadding(0, dp(2), 0, 0)
    }
    textContainer.addView(pkgView)

    row.addView(
      textContainer,
      LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
        marginStart = dp(12)
      }
    )

    return row
  }

  private fun buildEmptyMessage(): View = AppCompatTextView(context).apply {
    text = context.getString(R.string.snapshot_contribution_no_updates)
    textSize = 13f
    gravity = Gravity.CENTER
    minHeight = dp(52)
    setPadding(dp(16), dp(12), dp(16), dp(16))
    setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
  }

  fun show() {
    showAtLocation(host, Gravity.NO_GRAVITY, 0, 0)
    overlay.doOnLayout {
      if (isShowing && !closing) {
        positionSurface()
        animateTo(1f, 180L)
      }
    }
  }

  private fun positionSurface() {
    val origin = IntArray(2).also(overlay::getLocationOnScreen)
    val overlayWidth = overlay.width
    val overlayHeight = overlay.height
    if (overlayWidth <= 0 || overlayHeight <= 0) return

    val anchorCenterX = anchorRect.centerX() - origin[0]
    val anchorTopY = anchorRect.top - origin[1]
    val anchorBottomY = anchorRect.bottom - origin[1]

    val popupWidth = minOf(dp(270), overlayWidth - dp(32)).coerceAtLeast(dp(200))
    val maxAvailableHeight = (overlayHeight - dp(48)).coerceAtLeast(dp(100))

    surface.measure(
      View.MeasureSpec.makeMeasureSpec(popupWidth, View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec(maxAvailableHeight, View.MeasureSpec.AT_MOST)
    )
    val targetHeight = surface.measuredHeight

    val spaceBelow = overlayHeight - anchorBottomY - dp(16)
    val spaceAbove = anchorTopY - dp(16)

    val targetTop: Int
    if (spaceBelow >= targetHeight || spaceBelow >= spaceAbove) {
      surface.tailEdge = Gravity.TOP
      targetTop = (anchorBottomY + dp(4)).coerceIn(dp(16), (overlayHeight - targetHeight - dp(16)).coerceAtLeast(dp(16)))
    } else {
      surface.tailEdge = Gravity.BOTTOM
      targetTop = (anchorTopY - targetHeight - dp(4)).coerceIn(dp(16), (overlayHeight - targetHeight - dp(16)).coerceAtLeast(dp(16)))
    }

    val targetLeft = (anchorCenterX - popupWidth / 2).coerceIn(dp(16), (overlayWidth - popupWidth - dp(16)).coerceAtLeast(dp(16)))
    val relativeTailCenter = (anchorCenterX - targetLeft).toFloat().coerceIn(dp(20).toFloat(), (popupWidth - dp(20)).toFloat())
    surface.tailCenter = relativeTailCenter

    surface.layoutParams = (surface.layoutParams as FrameLayout.LayoutParams).apply {
      width = popupWidth
      height = targetHeight
      leftMargin = targetLeft
      topMargin = targetTop
    }
  }

  override fun dismiss() = close()

  fun close(after: (() -> Unit)? = null) {
    if (!isShowing || closing) return
    closing = true
    animateTo(0f, 120L) {
      dismissImmediately()
      after?.invoke()
    }
  }

  fun dismissImmediately() {
    animator?.removeAllListeners()
    animator?.cancel()
    animator = null
    super.dismiss()
  }

  private fun animateTo(target: Float, durationMs: Long, after: (() -> Unit)? = null) {
    animator?.removeAllListeners()
    animator?.cancel()
    animator = ValueAnimator.ofFloat(progress, target).apply {
      duration = (durationMs * kotlin.math.abs(target - progress)).roundToInt().toLong().coerceAtLeast(1L)
      interpolator = PathInterpolator(.2f, 0f, 0f, 1f)
      addUpdateListener { applyProgress(it.animatedValue as Float) }
      addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
          after?.invoke()
        }
      })
      start()
    }
  }

  private fun applyProgress(value: Float) {
    progress = value
    surface.alpha = value
    surface.scaleX = .96f + .04f * value
    surface.scaleY = surface.scaleX
    val offset = dp(8) * (1f - value)
    surface.translationY = if (surface.tailEdge == Gravity.TOP) -offset else offset
    overlay.setBackgroundColor(ColorUtils.setAlphaComponent(Color.BLACK, (60 * value).roundToInt()))
  }

  private fun dp(value: Int) = context.dpToDimension(value.toFloat()).roundToInt()

  private class MaxHeightScrollView(
    context: Context,
    private val maxHeightPx: Int
  ) : ScrollView(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      val heightMode = MeasureSpec.getMode(heightMeasureSpec)
      val heightSize = MeasureSpec.getSize(heightMeasureSpec)
      val constrainedHeightSpec = when (heightMode) {
        MeasureSpec.UNSPECIFIED -> MeasureSpec.makeMeasureSpec(maxHeightPx, MeasureSpec.AT_MOST)
        else -> MeasureSpec.makeMeasureSpec(minOf(heightSize, maxHeightPx), MeasureSpec.AT_MOST)
      }
      super.onMeasure(widthMeasureSpec, constrainedHeightSpec)
    }
  }

  private class BalloonView(context: Context) : FrameLayout(context) {
    private val path = Path()
    private val tailTransform = Matrix()
    private val radius = context.dpToDimension(20f)
    private val tailHeight = context.dpToDimension(8f)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = context.getColorByAttr(com.google.android.material.R.attr.colorSurfaceContainerHigh)
    }

    var tailCenter = 0f
      set(value) {
        field = value
        updatePath()
      }

    var tailEdge = Gravity.BOTTOM
      set(value) {
        if (field == value) return
        field = value
        setPadding(
          0,
          if (value == Gravity.TOP) tailHeight.roundToInt() else 0,
          0,
          if (value == Gravity.BOTTOM) tailHeight.roundToInt() else 0
        )
        updatePath()
      }

    init {
      setWillNotDraw(false)
      setPadding(0, 0, 0, tailHeight.roundToInt())
      elevation = context.dpToDimension(12f)
      outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
          if (OsUtils.atLeastR()) {
            outline.setPath(path)
          } else {
            outline.setRoundRect(
              0,
              if (tailEdge == Gravity.TOP) tailHeight.roundToInt() else 0,
              width,
              height - if (tailEdge == Gravity.BOTTOM) tailHeight.roundToInt() else 0,
              radius
            )
          }
        }
      }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
      updatePath()
    }

    private fun updatePath() {
      val h = height.toFloat()
      val w = width.toFloat()
      if (w <= 0f || h <= 0f) return
      if (tailEdge == Gravity.BOTTOM) {
        path.setG2Shape(0f, 0f, w, h - tailHeight, radius, tailCenter, radius * 1.8f, tailHeight)
        pivotX = tailCenter.coerceIn(0f, w)
        pivotY = h
      } else {
        path.setG2Shape(0f, 0f, w, h - tailHeight, radius, w - tailCenter, radius * 1.8f, tailHeight)
        tailTransform.setRotate(180f)
        tailTransform.postTranslate(w, h)
        path.transform(tailTransform)
        pivotX = tailCenter.coerceIn(0f, w)
        pivotY = 0f
      }
      invalidateOutline()
      invalidate()
    }

    override fun onDraw(canvas: Canvas) {
      canvas.drawPath(path, paint)
    }

    override fun dispatchDraw(canvas: Canvas) {
      val checkpoint = canvas.save()
      canvas.clipPath(path)
      super.dispatchDraw(canvas)
      canvas.restoreToCount(checkpoint)
    }
  }
}
