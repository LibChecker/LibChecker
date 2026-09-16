package com.absinthe.libchecker.domain.snapshot.timenode.ui.view

import android.animation.Keyframe
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.GestureDetector
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.R as AppCompatR
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.animation.doOnEnd
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.isVisible
import androidx.customview.widget.ExploreByTouchHelper
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.timenode.model.DayContribution
import com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotContributionData
import com.absinthe.libchecker.domain.snapshot.timenode.usecase.ComputeSnapshotContributionsUseCase
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.google.android.material.R as MaterialR
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.shape.SuperEllipseCornerTreatment
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class SnapshotContributionWallView(context: Context) : LinearLayout(context) {

  private var contributionData: SnapshotContributionData? = null
  private var currentSnapshotTimestamp: Long? = null
  private var onDayClickListener: ((DayContribution, Rect) -> Unit)? = null
  private val dayDetailFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())

  private val titleView = AppCompatTextView(
    ContextThemeWrapper(context, R.style.TextView_SansSerifCondensedMedium)
  ).apply {
    layoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
    text = context.getString(
      R.string.snapshot_contribution_recent_days,
      ComputeSnapshotContributionsUseCase.RECENT_DAYS_COUNT
    )
  }

  private val statsView = AppCompatTextView(context).apply {
    layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    setTextColor(context.getColorByAttr(android.R.attr.textColorSecondary))
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
  }

  private val headerLayout = LinearLayout(context).apply {
    orientation = HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
    val padding = 12.dp
    setPadding(padding, 4.dp, padding, 4.dp)
    addView(titleView)
    addView(statsView)
  }

  private val gridView = ContributionGridView(context)

  private val detailView = AppCompatTextView(context).apply {
    layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also {
      val padding = 12.dp
      setPadding(padding, 4.dp, padding, 8.dp)
    }
    setTextColor(context.getColorByAttr(android.R.attr.textColorSecondary))
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
    gravity = Gravity.CENTER
    text = context.getString(R.string.snapshot_contribution_hint)
  }

  init {
    orientation = VERTICAL
    val cardPadding = 12.dp
    setPadding(cardPadding, 4.dp, cardPadding, 4.dp)
    addView(headerLayout)
    addView(gridView)
    addView(detailView)
  }

  fun bind(
    data: SnapshotContributionData,
    selectedDate: LocalDate? = null
  ) {
    contributionData = data
    isVisible = data.days.isNotEmpty()
    statsView.text = resources.getQuantityString(
      R.plurals.snapshot_contribution_updates_count,
      data.totalUpdates,
      data.totalUpdates
    )

    val targetTimestamp = selectedDate?.let { data.days[it]?.snapshotTimestamp }
    if (currentSnapshotTimestamp != null && targetTimestamp != null && currentSnapshotTimestamp != targetTimestamp) {
      gridView.animateSnapshotHighlight(targetTimestamp)
    }
    if (targetTimestamp != null) {
      currentSnapshotTimestamp = targetTimestamp
    }

    gridView.setData(data, selectedDate)
    updateDetailText(selectedDate?.let { data.days[it] })
  }

  fun scrollToSnapshot(timestamp: Long) {
    val data = contributionData ?: return
    val targetDate = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    gridView.setSelectedDate(targetDate)
    updateDetailText(data.days[targetDate])
    if (currentSnapshotTimestamp != timestamp) {
      currentSnapshotTimestamp = timestamp
      gridView.animateSnapshotHighlight(timestamp)
    }
  }

  fun highlightSnapshot(timestamp: Long) {
    currentSnapshotTimestamp = timestamp
    gridView.animateSnapshotHighlight(timestamp)
  }

  fun setOnDayClickListener(listener: (DayContribution, Rect) -> Unit) {
    onDayClickListener = listener
  }

  private fun updateDetailText(dayContribution: DayContribution?) {
    if (dayContribution == null) {
      detailView.text = context.getString(R.string.snapshot_contribution_hint)
      return
    }

    detailView.text = describeDay(dayContribution)
  }

  private fun describeDay(dayContribution: DayContribution): String {
    val dateStr = dayDetailFormatter.format(dayContribution.date)
    val updateStr = resources.getQuantityString(
      R.plurals.snapshot_contribution_updates_count,
      dayContribution.updateCount,
      dayContribution.updateCount
    )

    return if (dayContribution.isSnapshotDay) {
      context.getString(R.string.snapshot_contribution_tile_detail_with_snapshot, dateStr, updateStr)
    } else {
      context.getString(R.string.snapshot_contribution_tile_detail, dateStr, updateStr)
    }
  }

  private inner class ContributionGridView(context: Context) : View(context) {

    private var data: SnapshotContributionData? = null
    private var selectedDate: LocalDate? = null
    private var animatingSnapshotTimestamp: Long? = null
    private var highlightProgress: Float = 0f
    private var highlightAnimator: ValueAnimator? = null

    private val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.FILL
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 2.dp.toFloat()
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.FILL
    }

    private val tilePath = Path()
    private val strokePath = Path()
    private val tileRect = RectF()
    private val radiiArray = FloatArray(8)

    private val defaultTileRadius = 4f * context.resources.displayMetrics.density
    private val largeCornerRadius = 14f * context.resources.displayMetrics.density
    private val tileGap = 3f * context.resources.displayMetrics.density

    private val gestureDetector = GestureDetector(
      context,
      object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onSingleTapUp(e: MotionEvent): Boolean {
          return activate(dayAt(e.x, e.y))
        }
      }
    )

    private val accessibility = object : ExploreByTouchHelper(this) {
      override fun getVirtualViewAt(x: Float, y: Float): Int = dayAt(x, y)

      override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>) {
        for (id in 0 until dayCount()) virtualViewIds.add(id)
      }

      override fun onPopulateNodeForVirtualView(virtualViewId: Int, node: AccessibilityNodeInfoCompat) {
        val day = dayForId(virtualViewId)
        node.contentDescription = day?.let(::describeDay) ?: context.getString(R.string.snapshot_contribution_hint)
        node.className = Button::class.java.name
        node.setBoundsInParent(boundsForDay(virtualViewId))
        node.isClickable = day != null
        node.isSelected = day != null && day.date == selectedDate
        if (day != null) node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
      }

      override fun onPerformActionForVirtualView(virtualViewId: Int, action: Int, arguments: Bundle?): Boolean {
        return action == AccessibilityNodeInfoCompat.ACTION_CLICK && activate(virtualViewId)
      }

      override fun onVirtualViewKeyboardFocusChanged(virtualViewId: Int, hasFocus: Boolean) {
        invalidate()
      }
    }

    private fun dayCount(): Int = data?.let {
      (ChronoUnit.DAYS.between(it.startDate, it.endDate).toInt() + 1).coerceAtLeast(0)
    } ?: 0

    private fun dayForId(id: Int): DayContribution? {
      val currentData = data ?: return null
      if (id !in 0 until dayCount()) return null
      val date = currentData.startDate.plusDays(id.toLong())
      return currentData.days[date] ?: DayContribution(date)
    }

    private fun tileSize(): Float {
      val cols = (dayCount() + 6) / 7
      return if (cols > 0) {
        ((width - paddingStart - paddingEnd - (cols - 1) * tileGap) / cols).coerceAtLeast(0f)
      } else {
        0f
      }
    }

    private fun boundsForDay(id: Int): Rect {
      val size = tileSize()
      val step = size + tileGap
      val left = paddingStart + id / 7 * step
      val top = paddingTop + id % 7 * step
      return Rect(left.toInt(), top.toInt(), (left + size).toInt(), (top + size).toInt())
    }

    private fun dayAt(x: Float, y: Float): Int {
      val size = tileSize()
      val step = size + tileGap
      val relX = x - paddingStart
      val relY = y - paddingTop
      if (size <= 0f || relX < 0f || relY < 0f) return ExploreByTouchHelper.INVALID_ID
      val col = (relX / step).toInt()
      val row = (relY / step).toInt()
      val id = col * 7 + row
      return if (col in 0 until (dayCount() + 6) / 7 && row in 0..6 &&
        relX % step < size && relY % step < size && dayForId(id) != null
      ) {
        id
      } else {
        ExploreByTouchHelper.INVALID_ID
      }
    }

    private fun activate(id: Int): Boolean {
      val day = dayForId(id) ?: return false
      selectedDate = day.date
      invalidate()
      accessibility.invalidateRoot()
      updateDetailText(day)
      val location = IntArray(2).also(::getLocationOnScreen)
      val anchor = boundsForDay(id).apply { offset(location[0], location[1]) }
      performClick()
      onDayClickListener?.invoke(day, anchor)
      accessibility.sendEventForVirtualView(id, AccessibilityEvent.TYPE_VIEW_CLICKED)
      return true
    }

    init {
      isFocusable = true
      isFocusableInTouchMode = true
      isClickable = true
      importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
      ViewCompat.setAccessibilityDelegate(this, accessibility)
      val hPadding = 12.dp
      val vPadding = 12.dp
      setPadding(hPadding, vPadding, hPadding, vPadding)
      val surfaceHigh = context.getColorByAttr(MaterialR.attr.colorSurfaceContainerHigh)
      background = MaterialShapeDrawable(
        ShapeAppearanceModel.builder()
          .setAllCorners(SuperEllipseCornerTreatment(22.dp.toFloat()))
          .build()
      ).apply {
        fillColor = ColorStateList.valueOf(surfaceHigh)
      }
    }

    fun setData(data: SnapshotContributionData, selected: LocalDate?) {
      this.data = data
      this.selectedDate = selected
      accessibility.invalidateRoot()
      requestLayout()
      invalidate()
    }

    fun setSelectedDate(date: LocalDate) {
      if (selectedDate != date) {
        selectedDate = date
        accessibility.invalidateRoot()
        invalidate()
      }
    }

    fun animateSnapshotHighlight(timestamp: Long) {
      highlightAnimator?.cancel()
      animatingSnapshotTimestamp = timestamp
      highlightProgress = 0f

      val k0 = Keyframe.ofFloat(0f, 0f)
      val k1 = Keyframe.ofFloat(0.2f, 1f).apply {
        interpolator = DecelerateInterpolator()
      }
      val k2 = Keyframe.ofFloat(0.65f, 1f)
      val k3 = Keyframe.ofFloat(1f, 0f).apply {
        interpolator = AccelerateDecelerateInterpolator()
      }
      val pvh = PropertyValuesHolder.ofKeyframe("progress", k0, k1, k2, k3)

      highlightAnimator = ValueAnimator.ofPropertyValuesHolder(pvh).apply {
        duration = 1000L
        addUpdateListener { animator ->
          highlightProgress = animator.getAnimatedValue("progress") as Float
          invalidate()
        }
        doOnEnd {
          animatingSnapshotTimestamp = null
          highlightProgress = 0f
          invalidate()
        }
        start()
      }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      val width = MeasureSpec.getSize(widthMeasureSpec)
      val contribData = data
      if (contribData == null) {
        setMeasuredDimension(width, paddingTop + paddingBottom)
        return
      }

      val totalDays = ChronoUnit.DAYS.between(contribData.startDate, contribData.endDate).toInt() + 1
      val cols = (totalDays + 6) / 7
      val availableWidth = (width - paddingStart - paddingEnd).toFloat()
      val tileSize = if (cols > 0) (availableWidth - (cols - 1) * tileGap) / cols else 0f
      val calculatedHeight = (paddingTop + paddingBottom + 7 * tileSize + 6 * tileGap).toInt()
      setMeasuredDimension(width, calculatedHeight)
    }

    override fun onDraw(canvas: Canvas) {
      super.onDraw(canvas)
      val contribData = data ?: return

      val totalDays = ChronoUnit.DAYS.between(contribData.startDate, contribData.endDate).toInt() + 1
      val cols = (totalDays + 6) / 7
      if (cols <= 0) return

      val availableWidth = (width - paddingStart - paddingEnd).toFloat()
      val tileSize = (availableWidth - (cols - 1) * tileGap) / cols
      val step = tileSize + tileGap

      val primaryColor = context.getColorByAttr(AppCompatR.attr.colorPrimary)
      val emptyColor = ColorUtils.setAlphaComponent(
        context.getColorByAttr(MaterialR.attr.colorOnSurface),
        20
      )

      for (col in 0 until cols) {
        for (row in 0..6) {
          val dayOffset = col * 7 + row
          val date = contribData.startDate.plusDays(dayOffset.toLong())

          val left = paddingStart + col * step
          val top = paddingTop + row * step
          val right = left + tileSize
          val bottom = top + tileSize
          tileRect.set(left, top, right, bottom)

          // Determine corner radii based on position in grid
          val tl = if (col == 0 && row == 0) largeCornerRadius else defaultTileRadius
          val bl = if (col == 0 && row == 6) largeCornerRadius else defaultTileRadius
          val tr = if (col == cols - 1 && row == 0) largeCornerRadius else defaultTileRadius
          val br = if (col == cols - 1 && row == 6) largeCornerRadius else defaultTileRadius

          radiiArray[0] = tl
          radiiArray[1] = tl
          radiiArray[2] = tr
          radiiArray[3] = tr
          radiiArray[4] = br
          radiiArray[5] = br
          radiiArray[6] = bl
          radiiArray[7] = bl

          tilePath.reset()
          tilePath.addRoundRect(tileRect, radiiArray, Path.Direction.CW)

          if (date.isAfter(contribData.endDate)) {
            // Future dates in the final week rendered as faint placeholder
            tilePaint.color = emptyColor
            canvas.drawPath(tilePath, tilePaint)
            continue
          }

          val dayContribution = contribData.days[date]
          val snapshotColor = dayContribution?.snapshotColor
          val count = dayContribution?.updateCount ?: 0
          val isSnapshotDay = dayContribution?.isSnapshotDay == true

          val fillColor = when {
            snapshotColor != null -> {
              if (isSnapshotDay) {
                snapshotColor
              } else {
                when {
                  count == 0 -> ColorUtils.setAlphaComponent(snapshotColor, 38)
                  count in 1..2 -> ColorUtils.setAlphaComponent(snapshotColor, 95)
                  count in 3..5 -> ColorUtils.setAlphaComponent(snapshotColor, 155)
                  count in 6..9 -> ColorUtils.setAlphaComponent(snapshotColor, 210)
                  else -> ColorUtils.setAlphaComponent(snapshotColor, 245)
                }
              }
            }

            else -> emptyColor
          }

          val isAnimating = dayContribution?.snapshotTimestamp == animatingSnapshotTimestamp && highlightProgress > 0f

          val finalFillColor = if (isAnimating) {
            val baseAlpha = Color.alpha(fillColor)
            val boostedAlpha = (baseAlpha + (255 - baseAlpha) * 0.6f * highlightProgress).toInt().coerceIn(0, 255)
            val boostedColor = ColorUtils.setAlphaComponent(fillColor, boostedAlpha)
            ColorUtils.blendARGB(boostedColor, Color.WHITE, 0.4f * highlightProgress)
          } else {
            fillColor
          }

          tilePaint.color = finalFillColor
          canvas.drawPath(tilePath, tilePaint)

          // Draw snapshot day centered dot
          if (isSnapshotDay) {
            val dotRadius = (tileSize * 0.16f).coerceIn(1.5.dp.toFloat(), 3.5.dp.toFloat())
            val dotCenterX = left + tileSize / 2f
            val dotCenterY = top + tileSize / 2f
            dotPaint.color = if (ColorUtils.calculateLuminance(finalFillColor) < 0.5) Color.WHITE else Color.BLACK
            canvas.drawCircle(dotCenterX, dotCenterY, dotRadius, dotPaint)
          }

          // Draw selection outline
          if (date == selectedDate || dayOffset == accessibility.keyboardFocusedVirtualViewId) {
            strokePaint.color = primaryColor
            strokePath.reset()
            strokePath.addRoundRect(tileRect, radiiArray, Path.Direction.CW)
            canvas.drawPath(strokePath, strokePaint)
          }
        }
      }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
      val handled = gestureDetector.onTouchEvent(event)
      return handled || super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
      super.performClick()
      return true
    }

    override fun dispatchHoverEvent(event: MotionEvent): Boolean = accessibility.dispatchHoverEvent(event) || super.dispatchHoverEvent(event)

    override fun dispatchKeyEvent(event: KeyEvent): Boolean = accessibility.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
      super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
      accessibility.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
    }

    override fun onDetachedFromWindow() {
      highlightAnimator?.cancel()
      highlightAnimator = null
      super.onDetachedFromWindow()
    }
  }
}
