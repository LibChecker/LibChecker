package com.absinthe.libchecker.domain.statistics.reference.ui.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.AttributeSet
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityEvent
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.customview.widget.ExploreByTouchHelper
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.statistics.reference.model.LibReference
import com.absinthe.libchecker.domain.statistics.reference.model.buildLibReferenceTreemapData
import com.absinthe.libchecker.domain.statistics.reference.model.resolveReferenceIcon
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.view.drawable.setG2Shape
import com.google.android.material.R as MaterialR

class LibReferenceTreemapView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  private val zoomEnabled: Boolean = true
) : View(context, attrs) {
  var onReferenceClick: (LibReference) -> Unit = {}
  var colorfulRuleIcon: Boolean = true
    set(value) {
      if (field == value) return
      field = value
      iconStyles.clear()
      rebuildCells()
    }

  private data class Cell(
    val id: String,
    val reference: LibReference?,
    val bounds: RectF,
    val label: String,
    val lines: List<String>,
    val count: String,
    val description: String,
    val contentPadding: Float,
    val contentWidth: Float,
    val contentHeight: Float,
    var contentScale: Float,
    val style: IconStyle,
    var contentOpacity: Float = 1f,
    var topFraction: Float = 0f,
    var bottomFraction: Float = 1f,
    var fadeTarget: Float = contentOpacity,
    var fadeFrom: Float = contentOpacity,
    var fadeStartTime: Long = 0L
  )

  private var pinchHintAnimator: ValueAnimator? = null
  private var pinchHintProgress = 0f
  private val pinchHintInterpolator = android.view.animation.PathInterpolator(0.4f, 0f, 0.2f, 1f)

  fun showPinchHint(): Boolean {
    if (!isShown || width == 0 || height == 0 || cells.isEmpty() || pinchHintAnimator != null) return false
    var ancestor: View? = this
    while (ancestor != null) {
      if (ancestor.alpha < 1f) return false
      ancestor = ancestor.parent as? View
    }
    pinchHintAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
      duration = 2600L
      repeatCount = ValueAnimator.INFINITE
      interpolator = android.view.animation.LinearInterpolator()
      addUpdateListener {
        pinchHintProgress = ((it.animatedValue as Float) * 2600f / 1600f).coerceAtMost(1f)
        invalidate()
      }
      addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
          pinchHintAnimator = null
          invalidate()
        }
      })
      start()
    }
    return true
  }

  private fun drawPinchHint(canvas: Canvas) {
    if (pinchHintAnimator == null) return
    val progress = pinchHintProgress
    val opacity = pinchHintInterpolator.getInterpolation(minOf(progress / 0.15f, (1f - progress) / 0.2f, 1f).coerceAtLeast(0f))
    val motion = pinchHintInterpolator.getInterpolation(((progress - 0.15f) / 0.65f).coerceIn(0f, 1f))
    val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val availableSpan = width * (if (landscape) 0.25f else 0.5f) - 26 * density
    val span = minOf(availableSpan.coerceAtLeast(0f), 80 * density) * (0.35f + motion * 0.65f)
    val centerX = width * if (landscape) 0.75f else 0.5f
    val centerY = height * if (landscape) 0.5f else 0.72f
    for (direction in -1..1 step 2) {
      val x = centerX + direction * span
      val y = centerY - direction * span * 0.35f
      fill.color = Color.BLACK
      fill.alpha = (opacity * 150).toInt()
      canvas.drawCircle(x, y, 17 * density, fill)
      fill.color = Color.WHITE
      fill.alpha = (opacity * 245).toInt()
      canvas.drawCircle(x, y, 12 * density, fill)
      fill.alpha = (opacity * 100).toInt()
      fill.style = Paint.Style.STROKE
      fill.strokeWidth = 2 * density
      canvas.drawCircle(x, y, (20 + motion * 4) * density, fill)
      fill.style = Paint.Style.FILL
    }
    fill.alpha = 255
  }

  private val density = resources.displayMetrics.density
  private val outerPath = Path()
  private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
  private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textSize = 14 * resources.displayMetrics.scaledDensity
    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
  }
  private val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textSize = 12 * resources.displayMetrics.scaledDensity
  }
  private data class IconStyle(val drawable: Drawable, val background: Int, val foreground: Int)
  private val surfaceColor = context.getColorByAttr(MaterialR.attr.colorSurfaceContainer)
  private val onSurfaceColor = context.getColorByAttr(MaterialR.attr.colorOnSurface)
  private val iconStyles = mutableMapOf<Pair<Int, Boolean>, IconStyle>()

  private fun iconStyle(iconRes: Int, simple: Boolean): IconStyle = iconStyles.getOrPut(iconRes to simple) {
    val drawable = requireNotNull(AppCompatResources.getDrawable(context, iconRes)).mutate()
    val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
    drawable.setBounds(0, 0, 32, 32)
    drawable.draw(Canvas(bitmap))
    val pixels = IntArray(32 * 32)
    bitmap.getPixels(pixels, 0, 32, 0, 0, 32, 32)
    bitmap.recycle()
    val accent = if (simple) null else TreemapIconColor.dominantColor(pixels)
    if (simple) {
      drawable.setTint(onSurfaceColor)
    } else if (!colorfulRuleIcon) {
      drawable.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
    }
    val background = accent?.let { color ->
      val hsl = FloatArray(3)
      ColorUtils.colorToHSL(color, hsl)
      hsl[1] *= 0.6f
      ColorUtils.HSLToColor(hsl)
    } ?: onSurfaceColor
    val foreground = if (ColorUtils.calculateContrast(Color.BLACK, background) >= ColorUtils.calculateContrast(Color.WHITE, background)) {
      Color.BLACK
    } else {
      Color.WHITE
    }
    IconStyle(drawable, background, foreground)
  }
  private var references = emptyList<LibReference>()
  private var items = buildLibReferenceTreemapData(references).associateBy { it.id }
  private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
  private var downX = 0f
  private var downY = 0f
  private var cells = emptyList<Cell>()
  private var animatedCell = ExploreByTouchHelper.INVALID_ID
  private var pressScale = 1f
  private var pressAnimator: ValueAnimator? = null
  private var pressedCell = ExploreByTouchHelper.INVALID_ID
  private var zoom = 1f
  private var transformedGesture = false
  private var zoomAnimator: ValueAnimator? = null
  private var visibleIds: Set<String>? = null
  private val zoomHistory = mutableListOf<Set<String>?>()
  private var partition: TreemapPartition? = null
  private var partitionWidth = 0
  private var partitionHeight = 0
  private var previewCells: List<Cell>? = null
  private data class TransitionCell(val outgoing: Cell, val incoming: Cell, val fromAlpha: Float, val toAlpha: Float, val sourceIndex: Int)
  private var transitionCells = emptyMap<String, TransitionCell>()
  private var previewIds: Set<String>? = null
  private var previewDirection = 0
  private var previewProgress = 0f
  private var textTransitionProgress = 0f
  private var zoomFocusX = 0f
  private var zoomFocusY = 0f
  private val scaleDetector = ScaleGestureDetector(
    context,
    object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
      override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        resetPressAnimation()
        transformedGesture = true
        zoomFocusX = detector.focusX
        zoomFocusY = detector.focusY
        return true
      }

      override fun onScale(detector: ScaleGestureDetector): Boolean {
        val next = (zoom * detector.scaleFactor).coerceIn(if (zoomHistory.isEmpty()) 1f else 0.5f, if (zoomHistory.size < 3) 2f else 1f)
        zoom = next
        updateZoomPreview()
        invalidate()
        return true
      }
    }
  )

  private fun updateZoomPreview() {
    val direction = if (zoom >= 1f) 1 else -1
    if (previewCells == null || previewDirection != direction) {
      val selected = if (direction > 0) {
        val region = RectF(zoomFocusX - width / 4f, zoomFocusY - height / 4f, zoomFocusX + width / 4f, zoomFocusY + height / 4f)
        cells.filter { region.contains(it.bounds.centerX(), it.bounds.centerY()) || it.bounds.contains(zoomFocusX, zoomFocusY) }.map { it.id }.toSet()
      } else {
        zoomHistory.lastOrNull()
      }
      if (direction > 0 && (selected.isNullOrEmpty() || selected.size == cells.size)) {
        clearPreview()
        return
      }
      previewDirection = direction
      previewIds = selected
      val target = createCells(selected)
      previewCells = target
      val old = cells.associateBy { it.id }
      val new = target.associateBy { it.id }
      transitionCells = (old.keys + new.keys).associateWith { id ->
        val outgoing = old[id] ?: new.getValue(id)
        val incoming = new[id] ?: old.getValue(id)
        TransitionCell(outgoing.copy(bounds = RectF()), incoming.copy(bounds = RectF()), old[id]?.contentOpacity ?: 0f, new[id]?.contentOpacity ?: 0f, cells.indexOfFirst { it.id == id })
      }
    }
    previewProgress = (if (direction > 0) zoom - 1f else (1f - zoom) * 2f).coerceIn(0f, 1f)
  }

  private fun settleZoom(cancelled: Boolean = false, commit: Boolean = previewProgress >= 0.5f) {
    val target = if (!cancelled && commit) 1f else 0f
    zoomAnimator = ValueAnimator.ofFloat(previewProgress, target).apply {
      duration = if (target == 1f) 330L else 180L
      interpolator = DecelerateInterpolator(2f)
      addUpdateListener {
        previewProgress = it.animatedValue as Float
        textTransitionProgress = if (target == 1f) ((it.currentPlayTime - 80L) / 250f).coerceIn(0f, 1f) else 0f
        invalidate()
      }
      addListener(object : AnimatorListenerAdapter() {
        private var cancelled = false
        override fun onAnimationCancel(animation: Animator) {
          this.cancelled = true
        }
        override fun onAnimationEnd(animation: Animator) {
          if (this.cancelled) return
          if (target == 1f && previewCells != null) {
            if (previewDirection > 0) zoomHistory.add(visibleIds) else zoomHistory.removeAt(zoomHistory.lastIndex)
            visibleIds = previewIds
            cells = checkNotNull(previewCells)
          }
          clearPreview()
          zoom = 1f
          accessibility.invalidateRoot()
          invalidate()
        }
      })
      start()
    }
  }

  private val accessibility = object : ExploreByTouchHelper(this) {
    override fun getVirtualViewAt(x: Float, y: Float): Int = cellAt(x, y)

    override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>) {
      cells.indices.forEach(virtualViewIds::add)
    }

    override fun onPopulateNodeForVirtualView(virtualViewId: Int, node: AccessibilityNodeInfoCompat) {
      val cell = cells.getOrNull(virtualViewId)
      node.contentDescription = cell?.description ?: "0"
      node.className = if (cell?.reference != null) Button::class.java.name else View::class.java.name
      node.setBoundsInParent(
        Rect().apply {
          cell?.bounds?.let { bounds ->
            bounds.roundOut(this)
          }
        }
      )
      node.isClickable = cell?.reference != null
      if (node.isClickable) node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
    }

    override fun onPerformActionForVirtualView(virtualViewId: Int, action: Int, arguments: Bundle?): Boolean {
      if (action != AccessibilityNodeInfoCompat.ACTION_CLICK) return false
      return activate(virtualViewId)
    }

    override fun onVirtualViewKeyboardFocusChanged(virtualViewId: Int, hasFocus: Boolean) {
      invalidate()
    }
  }

  init {
    isFocusable = true
    isClickable = true
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    ViewCompat.setAccessibilityDelegate(this, accessibility)
  }

  private data class FilterCell(val from: Cell, val to: Cell, val rendered: Cell, val incoming: Cell, val fromAlpha: Float, val toAlpha: Float, var alpha: Float = fromAlpha)
  private var filterCells = emptyList<FilterCell>()
  private var filterAnimator: ValueAnimator? = null

  private fun cancelFilterAnimation() {
    filterAnimator?.cancel()
    filterAnimator = null
    filterCells = emptyList()
  }

  fun submitReferences(references: List<LibReference>, animate: Boolean = false) {
    if (this.references == references) return
    val previous = if (filterCells.isEmpty()) cells.map { it to 1f } else filterCells.map { it.rendered.copy(bounds = RectF(it.rendered.bounds)) to it.alpha }
    this.references = references.toList()
    items = buildLibReferenceTreemapData(this.references).associateBy { it.id }
    partition = null
    visibleIds = null
    zoomHistory.clear()
    rebuildCells()
    if (animate) animateCellChanges(previous)
  }

  fun reflowForViewport() {
    val contentWidth = width - paddingLeft - paddingRight
    val contentHeight = height - paddingTop - paddingBottom
    if (contentWidth <= 0 || contentHeight <= 0 || (contentWidth == partitionWidth && contentHeight == partitionHeight)) return
    val previous = cells.map { it.copy(bounds = RectF(it.bounds)) to 1f }
    partition = null
    rebuildCells()
    animateCellChanges(previous)
  }

  private fun animateCellChanges(previous: List<Pair<Cell, Float>>) {
    if (isShown && width > 0 && height > 0 && (previous.isNotEmpty() || cells.isNotEmpty())) {
      pinchHintAnimator?.cancel()
      val old = previous.associateBy { it.first.id }
      val next = cells.associateBy { it.id }
      filterCells = (old.keys + next.keys).map { id ->
        val source = old[id]?.first ?: next.getValue(id)
        val target = next[id] ?: source
        FilterCell(source, target, source.copy(bounds = RectF(source.bounds)), target.copy(bounds = RectF(target.bounds)), old[id]?.second ?: 0f, if (id in next) 1f else 0f)
      }.sortedBy { it.toAlpha }
      filterAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 250L
        interpolator = pinchHintInterpolator
        addUpdateListener {
          val progress = it.animatedValue as Float
          filterCells.forEach { cell ->
            val from = cell.from.bounds
            val to = cell.to.bounds
            cell.rendered.bounds.set(
              from.left + (to.left - from.left) * progress,
              from.top + (to.top - from.top) * progress,
              from.right + (to.right - from.right) * progress,
              from.bottom + (to.bottom - from.bottom) * progress
            )
            updateContentScale(cell.rendered)
            cell.alpha = cell.fromAlpha + (cell.toAlpha - cell.fromAlpha) * progress
          }
          invalidate()
        }
        addListener(object : AnimatorListenerAdapter() {
          override fun onAnimationEnd(animation: Animator) {
            filterCells = emptyList()
            filterAnimator = null
            invalidate()
          }
        })
        start()
      }
    }
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    if (w == oldw && h > 0 && oldh > 0 && cells.isNotEmpty()) {
      cancelFilterAnimation()
      zoomAnimator?.cancel()
      zoom = 1f
      clearPreview()
      val contentHeight = (h - paddingTop - paddingBottom).coerceAtLeast(0)
      cells.forEach { cell ->
        val top = paddingTop + cell.topFraction * contentHeight
        val bottom = paddingTop + cell.bottomFraction * contentHeight
        val inset = minOf(1.5f * density, (bottom - top) / 4)
        cell.bounds.top = top + inset
        cell.bounds.bottom = bottom - inset
        updateContentScale(cell)
        updateContentFadeTarget(cell, if (cell.contentScale >= 0.4f) 1f else 0f)
      }
      startContentFadeIfNeeded()
      updateOuterPath()
      resetPressAnimation()
      accessibility.invalidateRoot()
      invalidate()
    } else {
      rebuildCells(animateContent = oldw > 0 && oldh > 0)
    }
  }

  override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
    val changed = left != paddingLeft || top != paddingTop || right != paddingRight || bottom != paddingBottom
    super.setPadding(left, top, right, bottom)
    if (changed && width > 0) rebuildCells()
  }

  private fun clearPreview() {
    previewCells = null
    transitionCells = emptyMap()
    previewProgress = 0f
    textTransitionProgress = 0f
  }

  private var contentFadeAnimator: ValueAnimator? = null

  private fun updateContentFadeTarget(cell: Cell, target: Float) {
    if (cell.fadeTarget == target) return
    cell.fadeFrom = cell.contentOpacity
    cell.fadeTarget = target
    cell.fadeStartTime = contentFadeAnimator?.currentPlayTime ?: 0L
  }

  private fun startContentFadeIfNeeded() {
    if (contentFadeAnimator != null || cells.none { it.contentOpacity != it.fadeTarget }) return
    contentFadeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
      duration = 150L
      repeatCount = ValueAnimator.INFINITE
      addUpdateListener { animator ->
        var unfinished = false
        cells.forEach { cell ->
          val fraction = ((animator.currentPlayTime - cell.fadeStartTime) / 150f).coerceIn(0f, 1f)
          val progress = pinchHintInterpolator.getInterpolation(fraction)
          cell.contentOpacity = cell.fadeFrom + (cell.fadeTarget - cell.fadeFrom) * progress
          if (cell.contentOpacity != cell.fadeTarget) unfinished = true
        }
        invalidate()
        if (!unfinished) cancel()
      }
      addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
          cells.forEach {
            it.contentOpacity = it.fadeTarget
            it.fadeFrom = it.fadeTarget
            it.fadeStartTime = 0L
          }
          contentFadeAnimator = null
          invalidate()
        }
      })
      start()
    }
  }

  private fun rebuildCells(animateContent: Boolean = false) {
    val previousOpacity = if (animateContent) cells.associate { it.id to it.contentOpacity } else emptyMap()
    contentFadeAnimator?.cancel()
    contentFadeAnimator = null
    cancelFilterAnimation()
    zoomAnimator?.cancel()
    zoom = 1f
    clearPreview()
    val contentWidth = width - paddingLeft - paddingRight
    val contentHeight = height - paddingTop - paddingBottom
    if (partition == null && contentWidth > 0 && contentHeight > 0) {
      val entries = items.values.map { TreemapLayout.Entry(it.id, it.count.toDouble(), it.reference == null) }
      partition = TreemapPartition(entries, contentWidth.toDouble(), contentHeight.toDouble())
      partitionWidth = contentWidth
      partitionHeight = contentHeight
    }
    cells = createCells(visibleIds)
    if (animateContent) {
      cells.forEach { cell ->
        val target = cell.contentOpacity
        cell.contentOpacity = previousOpacity[cell.id] ?: target
        cell.fadeTarget = cell.contentOpacity
        updateContentFadeTarget(cell, target)
      }
      startContentFadeIfNeeded()
    }
    updateOuterPath()
    resetPressAnimation()
    accessibility.invalidateRoot()
    invalidate()
  }

  private fun updateOuterPath() {
    val inset = 1.5f * density
    outerPath.setG2Shape(
      paddingLeft + inset,
      paddingTop + inset,
      width - paddingRight - inset,
      height - paddingBottom - inset,
      24 * density
    )
  }

  private fun createCells(
    ids: Set<String>?
  ): List<Cell> {
    val layout = partition?.layout(
      (width - paddingLeft - paddingRight).toDouble(),
      (height - paddingTop - paddingBottom).toDouble(),
      ids,
      ids,
      1f
    ).orEmpty()
    return layout.map { tile ->
      val item = items.getValue(tile.id)
      val reference = item.reference
      val bounds = RectF(
        tile.left.toFloat() + paddingLeft,
        tile.top.toFloat() + paddingTop,
        tile.right.toFloat() + paddingLeft,
        tile.bottom.toFloat() + paddingTop
      )
      bounds.inset(minOf(1.5f * density, bounds.width() / 4), minOf(1.5f * density, bounds.height() / 4))
      val label = reference?.rule?.label?.takeIf { it.isNotBlank() }
        ?: reference?.resolvedLabel?.takeIf { it.isNotBlank() }
        ?: reference?.let { it.libName.ifBlank { it.type.toString() } }
        ?: context.getString(R.string.lib_reference_other)
      val contentPadding = 8 * density
      val virtualWidth = maxOf(80 * density, bounds.width() - contentPadding * 2)
      val lines = labelLines(label, virtualWidth)
      val count = item.count.toString()
      val contentWidth = maxOf(32 * density, countPaint.measureText(count), lines.maxOf { labelPaint.measureText(it) })
      val contentHeight = 40 * density + lines.size * labelPaint.fontSpacing + countPaint.fontSpacing
      val contentScale = minOf(1f, bounds.width() / (contentWidth + contentPadding * 2), bounds.height() / (contentHeight + contentPadding * 2))
      val iconRes = reference?.let { resolveReferenceIcon(it.libName, it.type, it.rule) } ?: R.drawable.ic_reference_treemap
      val style = iconStyle(iconRes, reference?.rule?.isSimpleColorIcon != false)
      Cell(
        tile.id,
        reference,
        bounds,
        label,
        lines,
        count,
        if (reference == null) {
          context.getString(R.string.lib_reference_other_description, item.libraryCount, item.count)
        } else {
          "$label, ${item.count}"
        },
        contentPadding,
        contentWidth,
        contentHeight,
        contentScale,
        style,
        if (contentScale >= 0.4f) 1f else 0f,
        topFraction = (tile.top / (height - paddingTop - paddingBottom)).toFloat(),
        bottomFraction = (tile.bottom / (height - paddingTop - paddingBottom)).toFloat()
      )
    }
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    canvas.save()
    canvas.clipPath(outerPath)
    if (filterCells.isNotEmpty()) {
      val progress = filterAnimator?.animatedValue as? Float ?: 1f
      filterCells.forEach { cell ->
        val layer = canvas.saveLayerAlpha(cell.rendered.bounds, (cell.alpha * 255).toInt())
        val textProgress = ((progress - 0.4f) / 0.6f).coerceIn(0f, 1f)
        val sameContent = cell.from.lines == cell.to.lines && cell.from.count == cell.to.count
        cell.rendered.contentOpacity = if (sameContent) {
          cell.from.contentOpacity + (cell.to.contentOpacity - cell.from.contentOpacity) * progress
        } else {
          cell.from.contentOpacity * (1f - textProgress)
        }
        drawCell(canvas, cell.rendered, -1)
        if (!sameContent && textProgress > 0f) {
          val target = cell.incoming
          target.bounds.set(cell.rendered.bounds)
          target.contentOpacity = cell.to.contentOpacity * textProgress
          updateContentScale(target)
          drawCell(canvas, target, -1, drawBackground = false)
        }
        canvas.restoreToCount(layer)
      }
    } else if (previewCells == null) {
      cells.forEachIndexed { index, cell -> drawCell(canvas, cell, index) }
    } else {
      val layout = partition?.layout(
        (width - paddingLeft - paddingRight).toDouble(),
        (height - paddingTop - paddingBottom).toDouble(),
        visibleIds,
        previewIds,
        previewProgress
      ).orEmpty()
      layout.forEach { tile ->
        val transition = transitionCells.getValue(tile.id)
        val outgoing = transition.outgoing
        val incoming = transition.incoming
        outgoing.bounds.set(tile.left.toFloat() + paddingLeft, tile.top.toFloat() + paddingTop, tile.right.toFloat() + paddingLeft, tile.bottom.toFloat() + paddingTop)
        outgoing.bounds.inset(minOf(1.5f * density, outgoing.bounds.width() / 4), minOf(1.5f * density, outgoing.bounds.height() / 4))
        incoming.bounds.set(outgoing.bounds)
        updateContentScale(outgoing)
        updateContentScale(incoming)
        val sameLines = outgoing.lines == incoming.lines
        outgoing.contentOpacity = if (sameLines) {
          transition.fromAlpha + (transition.toAlpha - transition.fromAlpha) * previewProgress
        } else {
          transition.fromAlpha * (1f - textTransitionProgress)
        }
        incoming.contentOpacity = if (sameLines) 0f else transition.toAlpha * textTransitionProgress
        drawCell(canvas, outgoing, transition.sourceIndex)
        if (incoming.contentOpacity > 0f) drawCell(canvas, incoming, -1, drawBackground = false)
      }
    }
    drawPinchHint(canvas)
    canvas.restore()
  }

  private fun updateContentScale(cell: Cell) {
    cell.contentScale = minOf(1f, cell.bounds.width() / (cell.contentWidth + cell.contentPadding * 2), cell.bounds.height() / (cell.contentHeight + cell.contentPadding * 2))
  }

  private fun drawCell(canvas: Canvas, cell: Cell, index: Int, drawBackground: Boolean = true) {
    val bounds = cell.bounds
    canvas.save()
    if (index == animatedCell) canvas.scale(pressScale, pressScale, bounds.centerX(), bounds.centerY())
    if (drawBackground) {
      fill.color = cell.style.background
      canvas.drawRoundRect(bounds, 4 * density, 4 * density, fill)
      if (index == accessibility.keyboardFocusedVirtualViewId || index == accessibility.accessibilityFocusedVirtualViewId) {
        fill.color = cell.style.foreground
        fill.style = Paint.Style.STROKE
        fill.strokeWidth = 2 * density
        canvas.drawRoundRect(bounds, 4 * density, 4 * density, fill)
        fill.style = Paint.Style.FILL
      }
    }
    if (cell.contentOpacity <= 0f) {
      canvas.restore()
      return
    }
    canvas.clipRect(bounds)
    val contentLayer = canvas.saveLayerAlpha(bounds, (cell.contentOpacity * 255).toInt())
    labelPaint.color = cell.style.foreground
    countPaint.color = labelPaint.color
    canvas.translate(bounds.left, bounds.top)
    canvas.scale(cell.contentScale, cell.contentScale)
    val x = cell.contentPadding
    var y = cell.contentPadding
    fill.color = surfaceColor
    canvas.drawCircle(x + 16 * density, y + 16 * density, 16 * density, fill)
    cell.style.drawable.apply {
      setBounds((x + 4 * density).toInt(), (y + 4 * density).toInt(), (x + 28 * density).toInt(), (y + 28 * density).toInt())
      draw(canvas)
    }
    y += 40 * density
    cell.lines.forEach { line ->
      canvas.drawText(line, x, y - labelPaint.ascent(), labelPaint)
      y += labelPaint.fontSpacing
    }
    canvas.drawText(cell.count, x, y - countPaint.ascent(), countPaint)
    canvas.restoreToCount(contentLayer)
    canvas.restore()
  }

  private fun labelLines(label: String, width: Float): List<String> {
    if (width <= 0) return emptyList()
    if (labelPaint.measureText(label) <= width) return listOf(label)
    var split = labelPaint.breakText(label, true, width, null)
    if (split <= 0) return emptyList()
    if (split < label.length && Character.isLowSurrogate(label[split])) split--
    if (split <= 0) return emptyList()
    val space = label.lastIndexOf(' ', split - 1)
    if (space > 0) split = space
    val remainder = label.substring(split).trimStart()
    var end = labelPaint.breakText(remainder, true, width, null)
    if (end < remainder.length && Character.isLowSurrogate(remainder[end])) end--
    // Two lines at most; accessibility and search retain the complete library name.
    return listOf(label.substring(0, split).trimEnd(), remainder.take(end.coerceAtLeast(0)).trimEnd())
  }

  private fun cellAt(x: Float, y: Float): Int = cells.indexOfFirst { it.bounds.contains(x, y) }
    .takeIf { it >= 0 } ?: ExploreByTouchHelper.INVALID_ID

  override fun onGenericMotionEvent(event: MotionEvent): Boolean {
    if (!zoomEnabled || !isEnabled || event.actionMasked != MotionEvent.ACTION_SCROLL || !event.isFromSource(InputDevice.SOURCE_CLASS_POINTER)) {
      return super.onGenericMotionEvent(event)
    }
    val scroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
    if (!scroll.isFinite() || scroll == 0f) return super.onGenericMotionEvent(event)
    if (cells.isEmpty() || scaleDetector.isInProgress || zoomAnimator?.isRunning == true) return true
    if ((scroll > 0f && zoomHistory.size >= 3) || (scroll < 0f && zoomHistory.isEmpty())) return true
    pinchHintAnimator?.cancel()
    contentFadeAnimator?.end()
    filterAnimator?.end()
    resetPressAnimation()
    zoomFocusX = event.x
    zoomFocusY = event.y
    zoom = if (scroll > 0f) 2f else 0.5f
    updateZoomPreview()
    zoom = 1f
    if (previewCells != null) {
      previewProgress = 0f
      settleZoom(commit = true)
    }
    return true
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
      pinchHintAnimator?.cancel()
      contentFadeAnimator?.end()
      filterAnimator?.end()
      zoomAnimator?.end()
      transformedGesture = false
      parent?.requestDisallowInterceptTouchEvent(true)
    }
    if (zoomEnabled) scaleDetector.onTouchEvent(event)
    if (event.pointerCount > 1 || transformedGesture) {
      resetPressAnimation()
      transformedGesture = true
      if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
        if (zoomEnabled) settleZoom(cancelled = event.actionMasked == MotionEvent.ACTION_CANCEL)
        parent?.requestDisallowInterceptTouchEvent(false)
      }
      return true
    }
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        downX = event.x
        downY = event.y
        pressedCell = cellAt(event.x, event.y).takeIf { cells.getOrNull(it)?.reference != null }
          ?: ExploreByTouchHelper.INVALID_ID
        if (pressedCell != ExploreByTouchHelper.INVALID_ID) {
          if (animatedCell != pressedCell) pressScale = 1f
          animatedCell = pressedCell
          animatePress(true)
        }
        return true
      }

      MotionEvent.ACTION_MOVE -> {
        if (cellAt(event.x, event.y) != pressedCell ||
          kotlin.math.abs(event.x - downX) > touchSlop || kotlin.math.abs(event.y - downY) > touchSlop
        ) {
          if (pressedCell != ExploreByTouchHelper.INVALID_ID) animatePress(false)
          pressedCell = ExploreByTouchHelper.INVALID_ID
        }
      }

      MotionEvent.ACTION_UP -> {
        parent?.requestDisallowInterceptTouchEvent(false)
        val clicked = pressedCell
        pressedCell = ExploreByTouchHelper.INVALID_ID
        animatePress(false)
        if (clicked == cellAt(event.x, event.y) &&
          kotlin.math.abs(event.x - downX) <= touchSlop && kotlin.math.abs(event.y - downY) <= touchSlop
        ) {
          performClick()
          activate(clicked)
        }
      }

      MotionEvent.ACTION_CANCEL -> {
        parent?.requestDisallowInterceptTouchEvent(false)
        pressedCell = ExploreByTouchHelper.INVALID_ID
        animatePress(false)
      }
    }
    return true
  }

  private fun animatePress(down: Boolean) {
    pressAnimator?.cancel()
    pressAnimator = ValueAnimator.ofFloat(pressScale, if (down) 0.96f else 1f).apply {
      duration = if (down) 120L else 260L
      interpolator = if (down) DecelerateInterpolator(2f) else OvershootInterpolator(0.8f)
      addUpdateListener {
        pressScale = it.animatedValue as Float
        invalidate()
      }
      start()
    }
  }

  private fun resetPressAnimation() {
    pressAnimator?.cancel()
    pressAnimator = null
    pressedCell = ExploreByTouchHelper.INVALID_ID
    animatedCell = ExploreByTouchHelper.INVALID_ID
    pressScale = 1f
  }

  override fun onDetachedFromWindow() {
    contentFadeAnimator?.end()
    contentFadeAnimator = null
    cancelFilterAnimation()
    pinchHintAnimator?.cancel()
    resetPressAnimation()
    zoomAnimator?.cancel()
    clearPreview()
    zoom = 1f
    super.onDetachedFromWindow()
  }

  override fun performClick(): Boolean {
    super.performClick()
    return true
  }

  private fun activate(index: Int): Boolean {
    val reference = cells.getOrNull(index)?.reference ?: return false
    onReferenceClick(reference)
    accessibility.sendEventForVirtualView(index, AccessibilityEvent.TYPE_VIEW_CLICKED)
    return true
  }

  override fun dispatchHoverEvent(event: MotionEvent): Boolean = accessibility.dispatchHoverEvent(event) || super.dispatchHoverEvent(event)

  override fun dispatchKeyEvent(event: KeyEvent): Boolean = accessibility.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)

  override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
    super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
    accessibility.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
  }
}
