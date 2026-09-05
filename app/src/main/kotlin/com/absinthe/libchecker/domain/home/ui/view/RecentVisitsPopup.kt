package com.absinthe.libchecker.domain.home.ui.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.DragEvent
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.doOnLayout
import com.absinthe.libchecker.R
import com.absinthe.libchecker.ui.animator.ParticleRemoveItemAnimator
import com.absinthe.libchecker.ui.base.FIXED_BLUR_RADII
import com.absinthe.libchecker.ui.base.fixedBlurLayerAlphas
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.extensions.activity
import com.absinthe.libchecker.utils.extensions.dpToDimension
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.view.app.FloatingNavigationBar
import com.absinthe.libchecker.view.drawable.setG2Shape
import com.google.android.material.navigationrail.NavigationRailView
import java.util.UUID
import kotlin.math.roundToInt

class RecentVisitsPopup(
  private val host: View,
  private val navigation: View,
  private val anchor: View,
  val libraries: Boolean,
  private val onItemClick: (RecentVisitItem) -> Unit,
  private val onPin: (RecentVisitItem) -> Unit,
  private val onRemove: (RecentVisitItem) -> Unit
) : PopupWindow(host.context) {
  private val context = host.context
  private val sideNavigation = navigation is NavigationRailView
  private val overlay = FrameLayout(context)
  private val backdrop = BackdropView()
  private val surface = BalloonView(context)
  private val rows = LinearLayout(context).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(0, dp(8), 0, dp(8))
  }
  private var animator: ValueAnimator? = null
  private var resizeAnimator: ValueAnimator? = null
  private val surfaceBounds = Rect()
  private var progress = 0f
  private var closing = false
  private var items: RecentVisitGroup? = null
  private var draggedItem: RecentVisitItem? = null
  private var draggingRecent = false
  private var recentDropAccepted = false
  private var recentHeader: View? = null
  private var dragToken: String? = null
  private var dragPreview: RecentVisitDragPreview? = null
  private val dragOrigin = PointF()
  private var cancelDrag: (() -> Unit)? = null
  private var dropHint: View? = null
  private var dropHovered = false
  private var dropAnimator: ValueAnimator? = null
  private val dropBounds = Rect()
  private val sectionBounds = Rect()

  init {
    contentView = overlay
    width = ViewGroup.LayoutParams.MATCH_PARENT
    height = ViewGroup.LayoutParams.MATCH_PARENT
    isFocusable = true
    // The host is edge-to-edge; default popup clipping cuts the blur off at system bars.
    isClippingEnabled = false
    inputMethodMode = INPUT_METHOD_NOT_NEEDED
    animationStyle = 0
    setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    overlay.addView(backdrop, FrameLayout.LayoutParams(-1, -1))
    overlay.addView(surface, FrameLayout.LayoutParams(dp(264), -2))
    surface.addView(
      ScrollView(context).apply {
        isFillViewport = false
        isVerticalScrollBarEnabled = false
        addView(rows, ViewGroup.LayoutParams(-1, -2))
      },
      FrameLayout.LayoutParams(-1, -2)
    )
    overlay.setOnClickListener { dismiss() }
    overlay.setOnDragListener { _, event ->
      when (event.action) {
        DragEvent.ACTION_DRAG_STARTED ->
          draggedItem != null &&
            event.clipDescription?.label == dragToken && event.clipDescription?.hasMimeType(SHORTCUT_DRAG_MIME) == true

        DragEvent.ACTION_DRAG_LOCATION -> {
          updateDragLocation(event.x, event.y)
          true
        }

        DragEvent.ACTION_DRAG_EXITED -> {
          setDropHovered(false)
          true
        }

        DragEvent.ACTION_DROP -> {
          val item = draggedItem
          if (item != null && !closing && isInDropTarget(event.x, event.y)) {
            // Keep an internal drag's source attached until the native gesture finishes.
            if (draggingRecent) recentDropAccepted = true else pin(item)
            surface.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            true
          } else {
            false
          }
        }

        else -> true
      }
    }
    overlay.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
      if (oldRight > oldLeft && oldBottom > oldTop &&
        (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop)
      ) {
        dismissImmediately()
      }
    }
    surface.isClickable = true
    ViewCompat.setAccessibilityPaneTitle(
      surface,
      context.getString(if (libraries) R.string.recent_libraries else R.string.recent_apps)
    )
    setOnDismissListener { clearEffects() }
    applyProgress(0f)
  }

  fun show(items: RecentVisitGroup?) {
    updateItems(items)
    showAtLocation(host, Gravity.NO_GRAVITY, 0, 0)
    overlay.doOnLayout {
      if (isShowing && !closing) {
        val location = IntArray(2).also(overlay::getLocationOnScreen)
        dragPreview?.moveTo(dragOrigin.x - location[0], dragOrigin.y - location[1])
        positionSurface()
        dragPreview?.start()
        animateTo(1f, 200L)
        // Let the first window traversal submit its draw before PixelCopy occupies
        // RenderThread; otherwise that draw synchronously waits behind the copy.
        overlay.post {
          if (isShowing && !closing) {
            backdrop.capture {
              if (isShowing && !closing) {
                backdrop.setProgress(progress)
              }
            }
          }
        }
      }
    }
  }

  fun prepareDrag(item: RecentVisitItem, source: View, touch: PointF): String {
    draggedItem = item
    cancelDrag = source::cancelDragAndDrop
    val location = IntArray(2).also(source::getLocationOnScreen)
    dragOrigin.set(location[0] + touch.x, location[1] + touch.y)
    dragPreview = RecentVisitDragPreview(source, touch, item).also {
      // One hardware-rendered window draws both the morph and the popup. Updating a
      // native drag Surface every frame would synchronously wait on RenderThread.
      overlay.addView(it, FrameLayout.LayoutParams(-2, -2))
    }
    return UUID.randomUUID().toString().also { dragToken = it }
  }

  fun finishDrag(accepted: Boolean) {
    draggingRecent = false
    recentDropAccepted = false
    draggedItem = null
    dragToken = null
    cancelDrag = null
    dragPreview?.let { preview ->
      preview.stop()
      preview.animate().alpha(0f).setDuration(100L).withEndAction {
        overlay.removeView(preview)
        if (dragPreview === preview) dragPreview = null
      }.start()
    }
    setDropHovered(false)
    if (accepted) updateItems(items) else dismiss()
  }

  fun updateItems(items: RecentVisitGroup?) {
    this.items = items
    // Refresh after the drag ends so a data emission cannot detach its source mid-gesture.
    if (draggingRecent) return
    rows.removeAllViews()
    dropHint = null
    addHeader(R.string.shortcuts_pinned)
    val pinned = items?.pinned.orEmpty()
    if (draggedItem != null) {
      dropHint = addMessage(R.string.shortcuts_drop_hint).apply { alpha = .85f + .15f * surface.dropHighlight }
    } else if (pinned.isEmpty()) {
      addMessage(if (items == null) R.string.loading else R.string.shortcuts_drag_hint)
    }
    pinned.forEach { addItem(it, pinned = true) }
    recentHeader = addHeader(R.string.shortcuts_recent)
    val recent = items?.recent.orEmpty().filterNot { item -> pinned.any { it.visit.sameDestination(item.visit) } }
    if (recent.isEmpty()) addMessage(if (items == null) R.string.loading else R.string.recent_visits_empty)
    recent.take(5).forEach { addItem(it, pinned = false) }
    if (isShowing) overlay.post { if (isShowing && !closing) positionSurface() }
  }

  private fun addHeader(title: Int): View = AppCompatTextView(context).apply {
    setText(title)
    textSize = 12f
    setTypeface(typeface, android.graphics.Typeface.BOLD)
    setPadding(dp(16), dp(10), dp(16), dp(6))
    setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
    ViewCompat.setAccessibilityHeading(this, true)
    rows.addView(this, LinearLayout.LayoutParams(-1, -2))
  }

  private fun addMessage(message: Int): View = AppCompatTextView(context).apply {
    setText(message)
    textSize = 13f
    gravity = Gravity.CENTER_VERTICAL
    minHeight = dp(52)
    setPadding(dp(16), dp(10), dp(16), dp(10))
    setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
    rows.addView(this, LinearLayout.LayoutParams(-1, -2))
  }

  private fun addItem(item: RecentVisitItem, pinned: Boolean) {
    rows.addView(
      RecentVisitRow(context, item).apply {
        setOnClickListener { close { onItemClick(item) } }
        setOnHoldToRemove(onDrag = if (pinned) null else { touch -> startRecentDrag(this, item, touch) }) {
          removeRow(this, item)
        }
        if (!pinned) {
          ViewCompat.addAccessibilityAction(this, context.getString(R.string.shortcuts_pin)) { _, _ ->
            pin(item)
            true
          }
        }
        ViewCompat.replaceAccessibilityAction(
          this,
          AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_LONG_CLICK,
          context.getString(R.string.dialog_action_delete)
        ) { _, _ ->
          removeRow(this, item)
          true
        }
      },
      LinearLayout.LayoutParams(-1, -2)
    )
  }

  private fun removeRow(row: View, item: RecentVisitItem) {
    val current = items ?: return
    val entries = current.pinned + current.recent
    if (!isShowing || closing || entries.none { it.visit.sameDestination(item.visit) }) return
    // The solid snapshot fades by ~200 ms, before the 220 ms resize ends; particles trail off.
    ParticleRemoveItemAnimator.animateRemoval(row, overlay, durationMillis = 800L)
    updateItems(
      current.copy(
        pinned = current.pinned.filterNot { it.visit.sameDestination(item.visit) },
        recent = current.recent.filterNot { it.visit.sameDestination(item.visit) }
      )
    )
    onRemove(item)
  }

  private fun startRecentDrag(row: View, item: RecentVisitItem, touch: PointF): Boolean {
    if (!isShowing || closing || draggedItem != null) return false
    draggingRecent = true
    val token = prepareDrag(item, row, touch)
    val location = IntArray(2).also(overlay::getLocationOnScreen)
    val sourceLocation = IntArray(2)
    dragPreview?.moveTo(dragOrigin.x - location[0], dragOrigin.y - location[1])
    val started = startRecentVisitDrag(
      row,
      item,
      token,
      onEnd = { accepted ->
        if (dragToken == token) {
          val shouldPin = accepted && recentDropAccepted && !closing
          draggingRecent = false
          if (shouldPin) pin(item)
          // A cancelled internal drag leaves the popup open and restores the recent row.
          finishDrag(true)
        }
      },
      onLocation = { x, y ->
        // The source consumes drag events while the pointer is still over the recent row.
        row.getLocationOnScreen(sourceLocation)
        updateDragLocation(sourceLocation[0] + x - location[0], sourceLocation[1] + y - location[1])
      }
    )
    if (started) {
      dragPreview?.start()
      (rows.parent as ScrollView).smoothScrollTo(0, 0)
    } else {
      finishDrag(true)
    }
    return started
  }

  private fun updateDragLocation(x: Float, y: Float) {
    dragPreview?.moveTo(x, y)
    setDropHovered(isInDropTarget(x, y))
  }

  fun updateDragLocation(source: View, x: Float, y: Float) {
    val sourceLocation = IntArray(2).also(source::getLocationOnScreen)
    dragOrigin.set(sourceLocation[0] + x, sourceLocation[1] + y)
    if (!overlay.isLaidOut) return
    val origin = IntArray(2).also(overlay::getLocationOnScreen)
    updateDragLocation(dragOrigin.x - origin[0], dragOrigin.y - origin[1])
  }

  private fun isInDropTarget(x: Float, y: Float): Boolean {
    surface.getHitRect(dropBounds)
    if (draggingRecent) {
      val header = recentHeader ?: return false
      sectionBounds.set(0, 0, header.width, header.height)
      overlay.offsetDescendantRectToMyCoords(header, sectionBounds)
      dropBounds.bottom = minOf(dropBounds.bottom, sectionBounds.top)
    }
    return dropBounds.contains(x.roundToInt(), y.roundToInt())
  }

  private fun pin(item: RecentVisitItem) {
    val current = items ?: RecentVisitGroup()
    val others = current.pinned.filterNot { it.visit.sameDestination(item.visit) }
    draggedItem = null
    updateItems(current.copy(pinned = listOf(item) + others))
    onPin(item)
    surface.announceForAccessibility(context.getString(R.string.shortcuts_pinned_feedback, item.label))
  }

  private fun setDropHovered(hovered: Boolean) {
    if (dropHovered == hovered) return
    dropHovered = hovered
    dropAnimator?.cancel()
    dropAnimator = ValueAnimator.ofFloat(surface.dropHighlight, if (hovered) 1f else 0f).apply {
      duration = if (hovered) 160L else 120L
      interpolator = PathInterpolator(.2f, 0f, 0f, 1f)
      addUpdateListener {
        val fraction = it.animatedValue as Float
        surface.dropHighlight = fraction
        dropHint?.alpha = .85f + .15f * fraction
      }
      start()
    }
    if (hovered) surface.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
  }

  private fun positionSurface() {
    val origin = IntArray(2).also(overlay::getLocationOnScreen)
    val anchorIcon = anchor.findViewById<View>(com.google.android.material.R.id.navigation_bar_item_icon_view) ?: anchor
    val anchorLocation = IntArray(2).also(anchorIcon::getLocationOnScreen)
    val navLocation = IntArray(2).also(navigation::getLocationOnScreen)
    val visible = Rect().also(host::getWindowVisibleDisplayFrame)
    surface.tailEdge = when {
      !sideNavigation -> Gravity.BOTTOM
      navLocation[0] + navigation.width / 2 < visible.centerX() -> Gravity.LEFT
      else -> Gravity.RIGHT
    }
    val left = maxOf(
      maxOf(0, visible.left - origin[0]) + dp(16),
      if (surface.tailEdge == Gravity.LEFT) navLocation[0] + navigation.width - origin[0] + dp(4) else 0
    )
    val right = minOf(
      minOf(overlay.width, visible.right - origin[0]) - dp(16),
      if (surface.tailEdge == Gravity.RIGHT) navLocation[0] - origin[0] - dp(4) else overlay.width
    )
    val top = maxOf(0, visible.top - origin[1]) + dp(16)
    val bottom = if (!sideNavigation) {
      navLocation[1] - origin[1] - dp(4)
    } else {
      minOf(overlay.height, visible.bottom - origin[1]) - dp(16)
    }
    val popupWidth = minOf(dp(if (sideNavigation) 272 else 264), right - left).coerceAtLeast(1)
    val anchorX = anchorLocation[0] - origin[0] + anchorIcon.width / 2f
    val anchorY = anchorLocation[1] - origin[1] + anchorIcon.height / 2f
    val x = when (surface.tailEdge) {
      Gravity.LEFT -> left.toFloat()
      Gravity.RIGHT -> (right - popupWidth).toFloat()
      else -> (anchorX - popupWidth / 2f).coerceIn(left.toFloat(), maxOf(left, right - popupWidth).toFloat())
    }
    surface.measure(
      View.MeasureSpec.makeMeasureSpec(popupWidth, View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec((bottom - top).coerceAtLeast(1), View.MeasureSpec.AT_MOST)
    )
    val targetHeight = surface.measuredHeight
    val targetLeft = x.roundToInt()
    fun topForHeight(height: Int) = if (sideNavigation) {
      (anchorY - height / 2f).roundToInt().coerceIn(top, maxOf(top, bottom - height))
    } else {
      maxOf(top, bottom - height)
    }
    val targetTop = topForHeight(targetHeight)
    val targetBottom = targetTop + targetHeight
    val params = surface.layoutParams as FrameLayout.LayoutParams
    // Repeated data emissions should not restart an in-flight transition to the same bounds.
    if (surfaceBounds.left == targetLeft && surfaceBounds.top == targetTop && surfaceBounds.width() == popupWidth && surfaceBounds.height() == targetHeight) return
    surfaceBounds.set(targetLeft, targetTop, targetLeft + popupWidth, targetBottom)
    resizeAnimator?.cancel()
    fun applyHeight(height: Int) {
      val y = if (sideNavigation) topForHeight(height) else targetBottom - height
      surface.tailCenter = if (sideNavigation) anchorY - y else anchorX - targetLeft
      surface.layoutParams = params.apply {
        width = popupWidth
        this.height = height
        leftMargin = targetLeft
        topMargin = y
      }
    }
    if (progress > 0f && surface.isLaidOut && surface.height != targetHeight) {
      // ChangeBounds suppresses layout, leaving rebuilt rows blank during data refreshes.
      resizeAnimator = ValueAnimator.ofInt(surface.height, targetHeight).apply {
        duration = 220L
        interpolator = PathInterpolator(.2f, 0f, 0f, 1f)
        addUpdateListener { applyHeight(it.animatedValue as Int) }
        start()
      }
    } else {
      applyHeight(targetHeight)
    }
  }

  override fun dismiss() = close()

  private fun close(after: (() -> Unit)? = null) {
    if (!isShowing || closing) return
    closing = true
    animateTo(0f, 150L) {
      dismissImmediately()
      after?.invoke()
    }
  }

  fun dismissImmediately() {
    clearEffects()
    super.dismiss()
  }

  private fun clearEffects() {
    resizeAnimator?.cancel()
    resizeAnimator = null
    val cancel = cancelDrag
    cancelDrag = null
    draggingRecent = false
    recentDropAccepted = false
    draggedItem = null
    dragToken = null
    cancel?.invoke()
    dragPreview?.stop()
    dragPreview?.let(overlay::removeView)
    dragPreview = null
    dropAnimator?.cancel()
    dropAnimator = null
    dropHovered = false
    surface.dropHighlight = 0f
    animator?.removeAllListeners()
    animator?.cancel()
    animator = null
    applyProgress(0f)
    backdrop.release()
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
    surface.translationX = when (surface.tailEdge) {
      Gravity.LEFT -> -offset
      Gravity.RIGHT -> offset
      else -> 0f
    }
    surface.translationY = if (sideNavigation) 0f else offset
    backdrop.setProgress(value)
  }

  private fun dp(value: Int) = context.dpToDimension(value.toFloat()).roundToInt()

  private inner class BackdropView : FrameLayout(context) {
    private val contentClip = Path().apply { fillType = Path.FillType.EVEN_ODD }
    private val navigationCutout = Path()
    private val scrimPaint = Paint().apply { color = Color.BLACK }
    private val colorMatrix = ColorMatrix()
    private val ditherShader by lazy {
      if (OsUtils.atLeastT()) {
        RuntimeShader(
          """
          uniform shader content;
          uniform float strength;
          half4 main(float2 p) {
            half4 color = content.eval(p);
            float noise = fract(52.9829189 * fract(dot(p, float2(0.06711056, 0.00583715)))) - 0.5;
            return half4(clamp(color.rgb + noise * strength / 255.0, 0.0, 1.0), color.a);
          }
          """.trimIndent()
        )
      } else {
        null
      }
    }
    private var blurLayers = emptyList<ImageView>()
    private var amount = 0f

    fun capture(onReady: () -> Unit) {
      val origin = IntArray(2).also(::getLocationOnScreen)
      val hostLocation = IntArray(2).also(host::getLocationOnScreen)
      val navLocation = IntArray(2).also(navigation::getLocationOnScreen)
      val hostX = hostLocation[0] - origin[0]
      val hostY = hostLocation[1] - origin[1]
      val navX = (navLocation[0] - origin[0]).toFloat()
      val navY = (navLocation[1] - origin[1]).toFloat()
      // reset() preserves EVEN_ODD; rewind() resets it and turns the navigation cutout solid.
      contentClip.reset()
      contentClip.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
      val floatingProgress = (navigation as? FloatingNavigationBar)?.currentFloatingProgress ?: 0f
      if (floatingProgress > 0f) {
        // Match the fill path inside the translucent navigation stroke.
        val inset = resources.displayMetrics.density * floatingProgress / 2f
        navigationCutout.setG2Shape(
          navX + inset,
          navY + inset,
          navX + navigation.width - inset,
          navY + navigation.height - inset,
          (navigation.height / 2f - inset) * floatingProgress
        )
        contentClip.addPath(navigationCutout)
      } else {
        contentClip.addRect(navX, navY, navX + navigation.width, navY + navigation.height, Path.Direction.CW)
      }
      val window = context.activity?.window
      if (!OsUtils.atLeastS() || !host.isHardwareAccelerated || window == null) {
        onReady()
        return
      }
      val snapshot = runCatching {
        // Half-size sampling cuts the snapshot allocation/upload to a quarter;
        // scale the effect radii with the view to keep the same on-screen blur.
        Bitmap.createBitmap((host.width + 1) / 2, (host.height + 1) / 2, Bitmap.Config.ARGB_8888)
      }.getOrNull()
      if (snapshot == null) {
        onReady()
        return
      }
      val location = IntArray(2).also(host::getLocationInWindow)
      val source = Rect(location[0], location[1], location[0] + host.width, location[1] + host.height)
      try {
        // Freeze the composed window pixels, not live RenderNodes owned by another renderer.
        PixelCopy.request(window, source, snapshot, { result ->
          if (!isShowing || closing || result != PixelCopy.SUCCESS) {
            snapshot.recycle()
          } else {
            try {
              // Fixed kernels keep sample positions stable; animate only adjacent layer alphas.
              blurLayers = FIXED_BLUR_RADII.map { radius ->
                ImageView(context).apply {
                  setImageBitmap(snapshot)
                  scaleType = ImageView.ScaleType.FIT_XY
                  pivotX = 0f
                  pivotY = 0f
                  scaleX = host.width.toFloat() / snapshot.width
                  scaleY = host.height.toFloat() / snapshot.height
                  importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                  setRenderEffect(createBlurEffect(radius / FIXED_BLUR_RADII.last(), scaleX, scaleY))
                  alpha = 0f
                  addView(
                    this,
                    LayoutParams(snapshot.width, snapshot.height).apply {
                      leftMargin = hostX
                      topMargin = hostY
                    }
                  )
                }
              }
            } catch (_: RuntimeException) {
              release()
            }
          }
          onReady()
        }, Handler(Looper.getMainLooper()))
      } catch (_: RuntimeException) {
        snapshot.recycle()
        onReady()
      }
    }

    fun setProgress(value: Float) {
      amount = value
      if (OsUtils.atLeastS()) {
        val alphas = fixedBlurLayerAlphas(value * FIXED_BLUR_RADII.last())
        blurLayers.forEachIndexed { index, layer -> layer.alpha = alphas[index] }
      }
      invalidate()
    }

    private fun createBlurEffect(fraction: Float, scaleX: Float, scaleY: Float): RenderEffect {
      val radius = context.dpToDimension(20f) * fraction
      val blur = RenderEffect.createBlurEffect(radius / scaleX, radius / scaleY, Shader.TileMode.CLAMP)
      colorMatrix.setSaturation(1f - .25f * fraction)
      val toned = RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(colorMatrix), blur)
      if (!OsUtils.atLeastT()) return toned
      // Sub-pixel noise breaks up dark 8-bit blur bands without a visible grain texture.
      val shader = checkNotNull(ditherShader)
      shader.setFloatUniform("strength", 2f * fraction)
      return RenderEffect.createChainEffect(RenderEffect.createRuntimeShaderEffect(shader, "content"), toned)
    }

    override fun dispatchDraw(canvas: Canvas) {
      if (amount <= 0f) return
      val checkpoint = canvas.save()
      canvas.clipPath(contentClip)
      // The real navigation stays visible through this cutout, above both blur and dimming.
      super.dispatchDraw(canvas)
      scrimPaint.alpha = (amount * if (blurLayers.isEmpty()) 56f else 26f).roundToInt()
      canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)
      canvas.restoreToCount(checkpoint)
    }

    fun release() {
      blurLayers.forEach { it.setImageDrawable(null) }
      removeAllViews()
      blurLayers = emptyList()
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
    private val dropPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = context.getColorByAttr(androidx.appcompat.R.attr.colorPrimary)
      alpha = 0
    }
    var dropHighlight = 0f
      set(value) {
        field = value
        dropPaint.alpha = (20f * value).roundToInt()
        invalidate()
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
          if (value == Gravity.LEFT) tailHeight.roundToInt() else 0,
          0,
          if (value == Gravity.RIGHT) tailHeight.roundToInt() else 0,
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
            outline.setRoundRect(paddingLeft, 0, width - paddingRight, height - paddingBottom, radius)
          }
        }
      }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
      updatePath()
    }

    private fun updatePath() {
      if (tailEdge == Gravity.BOTTOM) {
        path.setG2Shape(0f, 0f, width.toFloat(), height - tailHeight, radius, tailCenter, radius * 1.8f, tailHeight)
        pivotX = tailCenter.coerceIn(0f, width.toFloat())
        pivotY = height.toFloat()
      } else {
        // Rotate the existing continuous curve rather than drawing a separate side pointer.
        val left = tailEdge == Gravity.LEFT
        path.setG2Shape(0f, 0f, height.toFloat(), width - tailHeight, radius, if (left) tailCenter else height - tailCenter, radius * 1.8f, tailHeight)
        tailTransform.setRotate(if (left) 90f else -90f)
        tailTransform.postTranslate(if (left) width.toFloat() else 0f, if (left) 0f else height.toFloat())
        path.transform(tailTransform)
        pivotX = if (left) 0f else width.toFloat()
        pivotY = tailCenter.coerceIn(0f, height.toFloat())
      }
      invalidateOutline()
      invalidate()
    }

    override fun onDraw(canvas: Canvas) {
      canvas.drawPath(path, paint)
      if (dropPaint.alpha > 0) canvas.drawPath(path, dropPaint)
    }

    override fun dispatchDraw(canvas: Canvas) {
      val checkpoint = canvas.save()
      canvas.clipPath(path)
      super.dispatchDraw(canvas)
      canvas.restoreToCount(checkpoint)
    }
  }
}
