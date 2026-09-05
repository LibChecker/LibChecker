package com.absinthe.libchecker.view.app

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.RequiresApi
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.animation.addListener
import androidx.core.graphics.withClip
import androidx.core.graphics.withScale
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.viewpager2.widget.ViewPager2
import com.absinthe.libchecker.R
import com.absinthe.libchecker.view.drawable.setG2Shape
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.motion.MotionUtils
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** Draws a progressive app bar blur and a uniform bottom navigation blur on Android 13+. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class BlurCoordinatorLayout @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  private val contentViewId: Int = R.id.viewpager
) : CoordinatorLayout(context, attrs) {

  private var contentNode: RenderNode? = null
  private var appbarSourceNode: RenderNode? = null
  private var navDownsampleNode: RenderNode? = null
  private var appbarEffectNode: RenderNode? = null
  private var navEffectNode: RenderNode? = null
  private var blurEffect: RenderEffect? = null
  private var navBlurEffect: RenderEffect? = null
  private var mediumBlurEffect: RenderEffect? = null
  private var lightBlurEffect: RenderEffect? = null
  private var progressiveBlurEffect: RenderEffect? = null
  private var progressiveRampTop = Float.NaN
  private var progressiveRampBottom = Float.NaN
  private val progressiveMaskShaders = arrayOfNulls<RuntimeShader>(PROGRESSIVE_MASK_COUNT)
  private var scrimPaint: Paint? = null
  private var appbarTintPaint: Paint? = null
  private var appbarTintTop = Float.NaN
  private var appbarTintBottom = Float.NaN
  private var appbarTintSurfaceColor = Color.TRANSPARENT
  private var appbarDarkMaskPaint: Paint? = null
  private var appbarDarkMaskTop = Float.NaN
  private var appbarDarkMaskBottom = Float.NaN
  private var appbarMaskAnimator: ValueAnimator? = null
  private var appbarMaskProgress = 0f
  private var blurAnimator: ValueAnimator? = null
  private var blurProgress = 0f
  private var contentUnderlapsAppbar = false
  private var originalNavElevation: Float = 0f
  private var floatingNavProgress: Float = 0f
  private val navClipPath = Path()
  private val navStrokePath = Path()
  private var originalViewPagerBehavior: CoordinatorLayout.Behavior<*>? = null
  private val contentBackgroundColor by lazy(LazyThreadSafetyMode.NONE) {
    resolveThemeColor(android.R.attr.colorBackground) ?: Color.BLACK
  }
  private val surfaceColor by lazy(LazyThreadSafetyMode.NONE) {
    resolveThemeColor(androidx.appcompat.R.attr.colorBackgroundFloating) ?: contentBackgroundColor
  }
  private val isNightMode by lazy(LazyThreadSafetyMode.NONE) {
    val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    nightMode == Configuration.UI_MODE_NIGHT_YES
  }
  private val outlineColor by lazy(LazyThreadSafetyMode.NONE) {
    resolveThemeColor(com.google.android.material.R.attr.colorOutline)
      ?.let { 0x40000000 or (it and 0x00FFFFFF) }
      ?: 0x33000000
  }

  var blurEnabled: Boolean = false
    private set

  fun setBlurEnabled(enabled: Boolean, onTransitionEnd: () -> Unit = {}) {
    val targetProgress = if (enabled) 1f else 0f
    if (blurAnimator == null && blurEnabled == enabled && blurProgress == targetProgress) {
      onTransitionEnd()
      return
    }

    blurAnimator?.cancel()
    if (!enabled) {
      updateBarScrollFlags(blurActive = false)
    }
    if (enabled && !blurEnabled) {
      blurEnabled = true
      updateAppbarMask(animate = isLaidOut)
      updateViewPagerBehavior()
      captureBarBackgrounds()
    }
    updateBarVisuals()

    if (!isLaidOut || blurProgress == targetProgress) {
      completeBlurTransition(enabled)
      onTransitionEnd()
      return
    }

    var cancelled = false
    blurAnimator = ValueAnimator.ofFloat(blurProgress, targetProgress).apply {
      duration = MotionUtils.resolveThemeDuration(
        context,
        com.google.android.material.R.attr.motionDurationLong2,
        BLUR_TRANSITION_DURATION_MS
      ).toLong()
      interpolator = MotionUtils.resolveThemeInterpolator(
        context,
        com.google.android.material.R.attr.motionEasingEmphasizedInterpolator,
        FastOutSlowInInterpolator()
      )
      addUpdateListener { animator ->
        blurProgress = animator.animatedValue as Float
        updateBarVisuals()
        invalidate()
      }
      addListener(
        onCancel = { cancelled = true },
        onEnd = {
          if (!cancelled) {
            blurAnimator = null
            completeBlurTransition(enabled)
            onTransitionEnd()
          }
        }
      )
      start()
    }
  }

  fun setAppbarContentUnderlap(underlaps: Boolean) {
    if (contentUnderlapsAppbar == underlaps) return
    contentUnderlapsAppbar = underlaps
    updateAppbarMask(animate = blurEnabled && isLaidOut)
  }

  fun setFloatingNavProgress(progress: Float) {
    if (floatingNavProgress == progress) return
    floatingNavProgress = progress
    if (blurEnabled) {
      val density = resources.displayMetrics.density
      originalNavElevation = (3f + 3f * progress) * density
      updateBarVisuals()
    }
    invalidate()
  }

  private fun updateAppbarMask(animate: Boolean) {
    val target = appbarMaskActivation(
      blurEnabled = blurEnabled,
      contentUnderlaps = contentUnderlapsAppbar
    )
    appbarMaskAnimator?.cancel()
    appbarMaskAnimator = null
    if (!animate || abs(appbarMaskProgress - target) < 0.001f) {
      appbarMaskProgress = target
      invalidate()
      return
    }
    appbarMaskAnimator = ValueAnimator.ofFloat(appbarMaskProgress, target).apply {
      duration = appbarMaskTransitionDuration(appbarMaskProgress, target)
      interpolator = LinearInterpolator()
      addUpdateListener { animator ->
        appbarMaskProgress = animator.animatedValue as Float
        invalidate()
      }
      start()
    }
  }

  private fun updateBarScrollFlags(blurActive: Boolean) {
    val appbar = findViewById<AppBarLayout>(R.id.appbar) ?: return
    appbar.isLiftOnScroll = !blurActive
    appbar.isLifted = !blurActive && contentUnderlapsAppbar

    appbar.bringToFront()
    val navView = findViewById<View>(R.id.nav_view) ?: return
    navView.bringToFront()
  }

  private fun updateViewPagerBehavior() {
    val viewPager = findViewById<ViewPager2>(R.id.viewpager) ?: return
    val lp = viewPager.layoutParams as? CoordinatorLayout.LayoutParams ?: return
    if (blurEnabled) {
      originalViewPagerBehavior = lp.behavior
      lp.behavior = null
    } else {
      lp.behavior = originalViewPagerBehavior
      originalViewPagerBehavior = null
    }
    viewPager.layoutParams = lp
    viewPager.requestLayout()
  }

  override fun dispatchDraw(canvas: Canvas) {
    if (!shouldUseRenderNodeBlur(blurEnabled, canvas.isHardwareAccelerated)) {
      super.dispatchDraw(canvas)
      return
    }

    val appbar = findViewById<AppBarLayout>(R.id.appbar)
    val navigation = findViewById<View>(R.id.nav_view)
    val navView = navigation as? BottomNavigationView
    val contentView = findViewById<View>(contentViewId)

    val capturedContent = if (contentView != null && width > 0 && height > 0) {
      val node = obtainContentNode()
      node.setRenderEffect(null)
      val recordCanvas = node.beginRecording(node.width, node.height)
      recordCanvas.drawColor(opaqueBackdropColor(contentBackgroundColor))
      drawCapturedContent(recordCanvas, contentView)
      node.endRecording()
      canvas.drawRenderNode(node)
      node
    } else {
      null
    }

    suppressManagedChildDraw = true
    try {
      super.dispatchDraw(canvas)
    } finally {
      suppressManagedChildDraw = false
    }

    capturedContent?.let { content ->
      if (appbar != null && appbar.visibility == VISIBLE && appbar.height > 0) {
        val appbarSource = obtainAppbarSourceNode(content, appbar.bottom)
        drawAppbarBlur(canvas, appbarSource)
      }
      if (navView != null && navView.visibility == VISIBLE && navView.alpha > 0f) {
        val navSource = obtainNavDownsampleNode(content, navView.top)
        drawNavBlur(canvas, navSource, navView)
      }
    }

    appbar?.let { drawChild(canvas, it, drawingTime) }
    navigation?.let { drawChild(canvas, it, drawingTime) }
  }

  private var suppressManagedChildDraw = false

  /**
   * Use the normal child drawing path so Android can reuse its display list and refresh dirty
   * descendants, while applying scroll, matrix, alpha and animation properties consistently.
   */
  private fun drawCapturedContent(canvas: Canvas, child: View) {
    if (child.visibility == VISIBLE || child.animation != null) {
      super.drawChild(canvas, child, drawingTime)
    }
  }

  public override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
    val isManagedChild =
      child.id == contentViewId || child.id == R.id.appbar || child.id == R.id.nav_view
    if (suppressManagedChildDraw && isManagedChild) {
      return false
    }
    return super.drawChild(canvas, child, drawingTime)
  }

  private fun obtainContentNode(): RenderNode {
    val targetW = width.coerceAtLeast(1)
    val targetH = height.coerceAtLeast(1)
    contentNode?.let { node ->
      if (node.width != targetW || node.height != targetH) {
        node.setPosition(0, 0, targetW, targetH)
      }
      return node
    }
    val node = RenderNode("LC.BlurContainer.content")
    node.setPosition(0, 0, targetW, targetH)
    contentNode = node
    return node
  }

  private fun obtainAppbarSourceNode(source: RenderNode, appbarBottom: Int): RenderNode {
    val downsample = APPBAR_BACKDROP_DOWNSAMPLE
    val targetW = (source.width / downsample).coerceAtLeast(1)
    val sourceH = (appbarBottom / downsample).coerceAtLeast(1)
    val contentOffset = calculateBackdropContentOffset(appbarBlurRadiusPx)
    val targetH = calculateBackdropTextureHeight(sourceH, contentOffset)
    appbarSourceNode?.let { node ->
      if (node.width == targetW && node.height == targetH && node.hasDisplayList()) {
        return node
      }
      recordBackdropNode(
        node = node,
        source = source,
        targetWidth = targetW,
        targetHeight = targetH,
        contentOffset = contentOffset,
        downsample = downsample
      )
      return node
    }
    return RenderNode("LC.BlurContainer.appbarSource").also { node ->
      recordBackdropNode(
        node = node,
        source = source,
        targetWidth = targetW,
        targetHeight = targetH,
        contentOffset = contentOffset,
        downsample = downsample
      )
      appbarSourceNode = node
    }
  }

  private fun obtainNavDownsampleNode(source: RenderNode, navTop: Int): RenderNode {
    val downsample = NAV_BACKDROP_DOWNSAMPLE
    val targetW = (source.width / downsample).coerceAtLeast(1)
    val sourceTop = (navTop / downsample).coerceAtLeast(0)
    val sourceH = ((source.height - navTop) / downsample).coerceAtLeast(1)
    val contentOffset = calculateBackdropContentOffset(NAV_BLUR_RADIUS_PX)
    val targetH = calculateBackdropTextureHeight(sourceH, contentOffset)
    navDownsampleNode?.let { node ->
      if (node.width == targetW && node.height == targetH && node.hasDisplayList()) {
        return node
      }
      recordBackdropNode(
        node = node,
        source = source,
        targetWidth = targetW,
        targetHeight = targetH,
        contentOffset = contentOffset,
        downsample = downsample,
        sourceTop = sourceTop
      )
      return node
    }
    return RenderNode("LC.BlurContainer.navSource").also { node ->
      recordBackdropNode(
        node = node,
        source = source,
        targetWidth = targetW,
        targetHeight = targetH,
        contentOffset = contentOffset,
        downsample = downsample,
        sourceTop = sourceTop
      )
      navDownsampleNode = node
    }
  }

  private fun recordBackdropNode(
    node: RenderNode,
    source: RenderNode,
    targetWidth: Int,
    targetHeight: Int,
    contentOffset: Int,
    downsample: Int,
    sourceTop: Int = 0
  ) {
    node.setPosition(0, 0, targetWidth, targetHeight)
    val canvas = node.beginRecording(targetWidth, targetHeight)
    canvas.drawColor(opaqueBackdropColor(contentBackgroundColor))
    canvas.translate(0f, (contentOffset - sourceTop).toFloat())
    canvas.scale(1f / downsample, 1f / downsample)
    canvas.drawRenderNode(source)
    node.endRecording()
  }

  private fun obtainAppbarEffectNode(source: RenderNode): RenderNode {
    appbarEffectNode?.let { node ->
      updateEffectNode(node, source)
      return node
    }
    return RenderNode("LC.BlurContainer.appbar").also { node ->
      updateEffectNode(node, source)
      appbarEffectNode = node
    }
  }

  private fun obtainNavEffectNode(source: RenderNode): RenderNode {
    navEffectNode?.let { node ->
      updateEffectNode(node, source)
      return node
    }
    return RenderNode("LC.BlurContainer.nav").also { node ->
      updateEffectNode(node, source)
      navEffectNode = node
    }
  }

  private fun updateEffectNode(node: RenderNode, source: RenderNode) {
    if (node.width == source.width && node.height == source.height && node.hasDisplayList()) {
      return
    }
    node.setPosition(0, 0, source.width, source.height)
    val canvas = node.beginRecording(source.width, source.height)
    canvas.drawRenderNode(source)
    node.endRecording()
  }

  private fun obtainBlurEffect(): RenderEffect {
    blurEffect?.let { return it }
    return RenderEffect.createBlurEffect(
      appbarBlurRadiusPx,
      appbarBlurRadiusPx,
      Shader.TileMode.CLAMP
    )
      .also { blurEffect = it }
  }

  private fun obtainMediumBlurEffect(): RenderEffect {
    mediumBlurEffect?.let { return it }
    return RenderEffect.createBlurEffect(
      appbarBlurRadiusPx * MEDIUM_BLUR_RADIUS_FRACTION,
      appbarBlurRadiusPx * MEDIUM_BLUR_RADIUS_FRACTION,
      Shader.TileMode.CLAMP
    )
      .also { mediumBlurEffect = it }
  }

  private fun obtainLightBlurEffect(): RenderEffect {
    lightBlurEffect?.let { return it }
    return RenderEffect.createBlurEffect(
      appbarBlurRadiusPx * LIGHT_BLUR_RADIUS_FRACTION,
      appbarBlurRadiusPx * LIGHT_BLUR_RADIUS_FRACTION,
      Shader.TileMode.CLAMP
    )
      .also { lightBlurEffect = it }
  }

  private fun obtainNavBlurEffect(): RenderEffect {
    navBlurEffect?.let { return it }
    return RenderEffect.createBlurEffect(
      NAV_BLUR_RADIUS_PX,
      NAV_BLUR_RADIUS_PX,
      Shader.TileMode.CLAMP
    )
      .also { navBlurEffect = it }
  }

  private fun drawAppbarBlur(canvas: Canvas, source: RenderNode) {
    val appbar = findViewById<AppBarLayout>(R.id.appbar) ?: return
    if (appbar.visibility != VISIBLE || appbar.height <= 0) return

    val top = appbar.top.toFloat()
    val bottom = appbar.bottom.toFloat()
    val downsample = APPBAR_BACKDROP_DOWNSAMPLE
    val contentOffset = appbarBackdropContentOffset
    val nodeRampTop = contentOffset + top / downsample
    val nodeRampBottom = contentOffset + bottom / downsample
    val effectNode = obtainAppbarEffectNode(source)

    effectNode.setRenderEffect(obtainProgressiveBlurEffect(nodeRampTop, nodeRampBottom))
    canvas.withClip(0f, top, width.toFloat(), bottom) {
      withScale(downsample.toFloat(), downsample.toFloat()) {
        translate(0f, -contentOffset)
        drawRenderNode(effectNode)
      }
      drawAppbarSurfaceTint(this, top, bottom)
      drawAppbarDarkMask(this, top, bottom)
    }
  }

  private fun drawAppbarSurfaceTint(canvas: Canvas, top: Float, bottom: Float) {
    if (bottom <= top || appbarMaskProgress <= 0f) return
    val tintColor = surfaceColor
    val paint = appbarTintPaint ?: Paint(Paint.ANTI_ALIAS_FLAG).also { appbarTintPaint = it }
    if (
      appbarTintTop != top ||
      appbarTintBottom != bottom ||
      appbarTintSurfaceColor != tintColor
    ) {
      val colors = IntArray(APPBAR_TINT_STOP_COUNT) { index ->
        val progress = index.toFloat() / (APPBAR_TINT_STOP_COUNT - 1)
        val alpha = (progressiveSurfaceTintAlpha(progress) * 255f).roundToInt()
        (tintColor and 0x00FFFFFF) or (alpha shl 24)
      }
      val positions = FloatArray(APPBAR_TINT_STOP_COUNT) { index ->
        index.toFloat() / (APPBAR_TINT_STOP_COUNT - 1)
      }
      paint.shader = LinearGradient(
        0f,
        top,
        0f,
        bottom,
        colors,
        positions,
        Shader.TileMode.CLAMP
      )
      appbarTintTop = top
      appbarTintBottom = bottom
      appbarTintSurfaceColor = tintColor
    }
    paint.alpha = (appbarMaskProgress * 255f).roundToInt()
    paint.blendMode = BlendMode.SRC_OVER
    canvas.drawRect(0f, top, width.toFloat(), bottom, paint)
  }

  private fun drawAppbarDarkMask(canvas: Canvas, top: Float, bottom: Float) {
    if (bottom <= top || appbarMaskProgress <= 0f) return
    val paint = appbarDarkMaskPaint ?: Paint(Paint.ANTI_ALIAS_FLAG).also {
      appbarDarkMaskPaint = it
    }
    if (appbarDarkMaskTop != top || appbarDarkMaskBottom != bottom) {
      val colors = IntArray(APPBAR_TINT_STOP_COUNT) { index ->
        val progress = index.toFloat() / (APPBAR_TINT_STOP_COUNT - 1)
        val alpha = (progressiveDarkMaskAlpha(progress) * 255f).roundToInt()
        alpha shl 24
      }
      val positions = FloatArray(APPBAR_TINT_STOP_COUNT) { index ->
        index.toFloat() / (APPBAR_TINT_STOP_COUNT - 1)
      }
      paint.shader = LinearGradient(
        0f,
        top,
        0f,
        bottom,
        colors,
        positions,
        Shader.TileMode.CLAMP
      )
      appbarDarkMaskTop = top
      appbarDarkMaskBottom = bottom
    }
    paint.alpha = (appbarMaskProgress * 255f).roundToInt()
    paint.blendMode = BlendMode.SRC_OVER
    canvas.drawRect(0f, top, width.toFloat(), bottom, paint)
  }

  /** Stacks strong, medium and light blur masks, then fades to the captured sharp content. */
  private fun obtainProgressiveBlurEffect(rampTop: Float, rampBottom: Float): RenderEffect {
    progressiveBlurEffect?.let { cached ->
      if (progressiveRampTop == rampTop && progressiveRampBottom == rampBottom) {
        return cached
      }
    }

    fun maskedEffect(
      shaderIndex: Int,
      level: Float,
      slope: Float,
      input: RenderEffect
    ): RenderEffect {
      val shader = progressiveMaskShaders[shaderIndex]
        ?: RuntimeShader(PROGRESSIVE_LEVEL_MASK_SHADER).also {
          progressiveMaskShaders[shaderIndex] = it
        }
      shader.setFloatUniform("in_gradBand", rampTop, rampBottom)
      shader.setFloatUniform("in_curve", APPBAR_PROGRESSIVE_CURVE)
      shader.setFloatUniform("in_level", level)
      shader.setFloatUniform("in_slope", slope)
      val mask = RenderEffect.createRuntimeShaderEffect(shader, "child")
      return RenderEffect.createChainEffect(mask, input)
    }

    val light = maskedEffect(
      shaderIndex = LIGHT_MASK_INDEX,
      level = LIGHT_MASK_LEVEL,
      slope = LEVEL_MASK_SLOPE,
      input = obtainLightBlurEffect()
    )
    val medium = maskedEffect(
      shaderIndex = MEDIUM_MASK_INDEX,
      level = MEDIUM_MASK_LEVEL,
      slope = LEVEL_MASK_SLOPE,
      input = obtainMediumBlurEffect()
    )
    val strong = maskedEffect(
      shaderIndex = STRONG_MASK_INDEX,
      level = STRONG_MASK_LEVEL,
      slope = LEVEL_MASK_SLOPE,
      input = obtainBlurEffect()
    )

    val stack = RenderEffect.createBlendModeEffect(
      RenderEffect.createBlendModeEffect(light, medium, BlendMode.SRC_OVER),
      strong,
      BlendMode.SRC_OVER
    )

    progressiveRampTop = rampTop
    progressiveRampBottom = rampBottom
    return stack.also { progressiveBlurEffect = it }
  }

  private fun drawNavBlur(
    canvas: Canvas,
    source: RenderNode,
    navView: BottomNavigationView
  ) {
    if (floatingNavProgress > 0f) {
      val left = (navView.left + navView.translationX).coerceAtLeast(0f)
      val right = (navView.right + navView.translationX).coerceAtMost(width.toFloat())
      val top = navView.top + navView.translationY
      val bottom = navView.bottom + navView.translationY
      if (top >= height || bottom <= top || left >= right) return

      val cornerRadius = ((bottom - top) / 2f) * floatingNavProgress
      navClipPath.setG2Shape(left, top, right, bottom, cornerRadius)
      canvas.withClip(navClipPath) {
        drawNavBackdrop(this, source, left, top, right, bottom)
      }

      val strokeWidth = dividerHeightPx()
      val strokePaint = obtainScrimPaint()
      strokePaint.shader = null
      strokePaint.style = Paint.Style.STROKE
      strokePaint.strokeWidth = strokeWidth
      strokePaint.blendMode = BlendMode.SRC_OVER
      val strokeAlpha = ((outlineColor ushr 24) * floatingNavProgress).roundToInt()
      strokePaint.color = (outlineColor and 0x00FFFFFF) or (strokeAlpha shl 24)
      val halfStroke = strokeWidth / 2f
      navStrokePath.setG2Shape(
        left + halfStroke,
        top + halfStroke,
        right - halfStroke,
        bottom - halfStroke,
        (cornerRadius - halfStroke).coerceAtLeast(0f)
      )
      canvas.drawPath(navStrokePath, strokePaint)
      drawNavDivider(canvas, top, floatingNavProgress)
    } else {
      val top = navView.top + navView.translationY
      if (top >= height) return
      val bottom = height.toFloat()

      canvas.withClip(0f, top, width.toFloat(), bottom) {
        drawNavBackdrop(this, source, 0f, top, width.toFloat(), bottom)
        drawNavDivider(this, top, 0f)
      }
    }
  }

  private fun drawNavBackdrop(
    canvas: Canvas,
    source: RenderNode,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float
  ) {
    val paint = obtainScrimPaint()
    paint.shader = null
    paint.style = Paint.Style.FILL
    paint.blendMode = BlendMode.SRC_OVER
    paint.color = surfaceColor and BASE_FILL_ALPHA_MASK.toInt()
    canvas.drawRect(left, top, right, bottom, paint)
    drawNavBlurredSource(canvas, source, top)
    drawSurfaceTint(canvas, top, bottom)
  }

  private fun drawNavDivider(canvas: Canvas, top: Float, floatingProgress: Float) {
    val alpha = fadingNavDividerAlpha(outlineColor ushr 24, floatingProgress)
    if (alpha == 0) return
    val paint = obtainScrimPaint()
    paint.shader = null
    paint.style = Paint.Style.FILL
    paint.blendMode = BlendMode.SRC_OVER
    paint.color = (outlineColor and 0x00FFFFFF) or (alpha shl 24)
    canvas.drawRect(0f, top, width.toFloat(), top + dividerHeightPx(), paint)
  }

  private fun drawSurfaceTint(canvas: Canvas, top: Float, bottom: Float) {
    val paint = obtainScrimPaint()
    paint.shader = null
    paint.blendMode = BlendMode.SRC_OVER
    paint.color = if (isNightMode) {
      DARK_NAV_TINT_ALPHA shl 24
    } else {
      (surfaceColor and 0x00FFFFFF) or (NAV_TINT_ALPHA shl 24)
    }
    canvas.drawRect(0f, top, width.toFloat(), bottom, paint)
  }

  private fun drawNavBlurredSource(canvas: Canvas, source: RenderNode, navTop: Float) {
    val effectNode = obtainNavEffectNode(source)
    effectNode.setRenderEffect(obtainNavBlurEffect())
    val downsample = NAV_BACKDROP_DOWNSAMPLE
    canvas.withScale(downsample.toFloat(), downsample.toFloat()) {
      translate(0f, navTop / downsample - navBackdropContentOffset)
      drawRenderNode(effectNode)
    }
  }

  private fun obtainScrimPaint(): Paint {
    return scrimPaint ?: Paint(Paint.ANTI_ALIAS_FLAG).also { scrimPaint = it }
  }

  private fun resolveThemeColor(attribute: Int): Int? {
    val typedValue = android.util.TypedValue()
    val resolved = context.theme.resolveAttribute(
      attribute,
      typedValue,
      true
    )
    return typedValue.data.takeIf { resolved }
  }

  private fun dividerHeightPx(): Float = DIVIDER_HEIGHT_DP * resources.displayMetrics.density

  private val appbarBlurRadiusPx by lazy(LazyThreadSafetyMode.NONE) {
    calculateDownsampledBlurRadius(
      blurRadiusDp = APPBAR_BLUR_RADIUS_DP,
      density = resources.displayMetrics.density,
      downsample = APPBAR_BACKDROP_DOWNSAMPLE
    )
  }
  private val appbarBackdropContentOffset by lazy(LazyThreadSafetyMode.NONE) {
    calculateBackdropContentOffset(appbarBlurRadiusPx).toFloat()
  }
  private val navBackdropContentOffset by lazy(LazyThreadSafetyMode.NONE) {
    calculateBackdropContentOffset(NAV_BLUR_RADIUS_PX).toFloat()
  }

  private fun captureBarBackgrounds() {
    val navView = findViewById<View>(R.id.nav_view)
    val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)

    findViewById<AppBarLayout>(R.id.appbar)?.background?.mutate()
    navView?.background?.mutate()
    toolbar?.background?.mutate()
    originalNavElevation = navView?.elevation ?: 0f
  }

  private fun completeBlurTransition(enabled: Boolean) {
    blurProgress = if (enabled) 1f else 0f
    updateBarVisuals()
    if (enabled) {
      updateBarScrollFlags(blurActive = true)
    }
    if (!enabled) {
      blurEnabled = false
      updateAppbarMask(animate = false)
      updateViewPagerBehavior()
      originalNavElevation = 0f
    }
    invalidate()
  }

  private fun updateBarVisuals() {
    val backgroundAlpha = blurBackgroundAlpha(blurProgress)
    findViewById<AppBarLayout>(R.id.appbar)?.background?.alpha = backgroundAlpha
    findViewById<View>(R.id.nav_view)?.let { navView ->
      navView.background?.alpha = backgroundAlpha
      navView.elevation = originalNavElevation * (1f - blurProgress)
      (navView as? FloatingNavigationBar)?.setBlurProgress(blurProgress)
    }
    findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)?.background?.alpha = backgroundAlpha
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    blurAnimator?.cancel()
    blurAnimator = null
    appbarMaskAnimator?.cancel()
    appbarMaskAnimator = null
    contentNode = null
    appbarSourceNode = null
    navDownsampleNode = null
    appbarEffectNode = null
    navEffectNode = null
    blurEffect = null
    mediumBlurEffect = null
    lightBlurEffect = null
    navBlurEffect = null
    progressiveBlurEffect = null
    progressiveMaskShaders.fill(null)
    scrimPaint = null
    appbarTintPaint = null
    appbarTintTop = Float.NaN
    appbarTintBottom = Float.NaN
    appbarTintSurfaceColor = Color.TRANSPARENT
    appbarDarkMaskPaint = null
    appbarDarkMaskTop = Float.NaN
    appbarDarkMaskBottom = Float.NaN
  }

  companion object {
    // Miuix's progressive app bar recipe uses a 10dp radius and a 30% surface blend.
    private const val APPBAR_BLUR_RADIUS_DP = 10f
    private const val NAV_BLUR_RADIUS_PX = 10f
    private const val MEDIUM_BLUR_RADIUS_FRACTION = 0.4f
    private const val LIGHT_BLUR_RADIUS_FRACTION = 0.13f
    private const val LEVEL_MASK_SLOPE = 3f
    private const val STRONG_MASK_LEVEL = 1f
    private const val MEDIUM_MASK_LEVEL = 2f
    private const val LIGHT_MASK_LEVEL = 3f
    private const val STRONG_MASK_INDEX = 0
    private const val MEDIUM_MASK_INDEX = 1
    private const val LIGHT_MASK_INDEX = 2
    private const val PROGRESSIVE_MASK_COUNT = 3
    private const val APPBAR_TINT_STOP_COUNT = 9
    private const val NAV_TINT_ALPHA = 0x99
    private const val DARK_NAV_TINT_ALPHA = 0xCC
    private const val BASE_FILL_ALPHA_MASK = 0xE6FFFFFFL // 90% opaque base so empty areas still look like glass
    private const val DIVIDER_HEIGHT_DP = 1f
  }
}

internal const val APPBAR_BACKDROP_DOWNSAMPLE = 1
internal const val NAV_BACKDROP_DOWNSAMPLE = 2

internal fun shouldUseRenderNodeBlur(
  blurEnabled: Boolean,
  canvasIsHardwareAccelerated: Boolean
): Boolean = blurEnabled && canvasIsHardwareAccelerated

internal fun calculateBackdropContentOffset(maxBlurRadius: Float): Int = ceil(maxBlurRadius.coerceAtLeast(0f) * BLUR_KERNEL_SIGMA_OUTSET).toInt()

internal fun calculateBackdropTextureHeight(sourceHeight: Int, contentOffset: Int): Int = sourceHeight.coerceAtLeast(0) + contentOffset.coerceAtLeast(0) * 2

internal fun calculateDownsampledBlurRadius(
  blurRadiusDp: Float,
  density: Float,
  downsample: Int
): Float {
  if (downsample <= 0) return 0f
  return blurRadiusDp.coerceAtLeast(0f) * density.coerceAtLeast(0f) / downsample
}

internal fun opaqueBackdropColor(color: Int): Int = color or 0xFF000000.toInt()

internal fun blurBackgroundAlpha(progress: Float): Int = ((1f - progress.coerceIn(0f, 1f)) * OPAQUE_LAYER_ALPHA).roundToInt()

internal fun fadingNavDividerAlpha(baseAlpha: Int, floatingProgress: Float): Int = (baseAlpha.coerceIn(0, 255) * (1f - floatingProgress.coerceIn(0f, 1f))).roundToInt()

internal fun progressiveSurfaceTintAlpha(
  progress: Float,
  maxAlpha: Float = APPBAR_SURFACE_TINT_ALPHA,
  curve: Float = APPBAR_PROGRESSIVE_CURVE
): Float {
  val shapedProgress = smoothStep(progress).pow(curve.coerceAtLeast(0f))
  return maxAlpha.coerceIn(0f, 1f) * (1f - shapedProgress)
}

internal fun appbarMaskActivation(
  blurEnabled: Boolean,
  contentUnderlaps: Boolean
): Float = if (blurEnabled && contentUnderlaps) 1f else 0f

internal fun appbarMaskTransitionDuration(
  start: Float,
  target: Float
): Long = (abs(target - start) * APPBAR_MASK_TRANSITION_DURATION_MS).roundToLong().coerceAtLeast(1L)

internal fun progressiveDarkMaskAlpha(
  progress: Float,
  maxAlpha: Float = APPBAR_DARK_MASK_ALPHA,
  curve: Float = APPBAR_PROGRESSIVE_CURVE
): Float {
  val shapedProgress = smoothStep(progress).pow(curve.coerceAtLeast(0f))
  return maxAlpha.coerceIn(0f, 1f) * (1f - shapedProgress)
}

private fun smoothStep(value: Float): Float {
  val clamped = value.coerceIn(0f, 1f)
  return clamped * clamped * (3f - 2f * clamped)
}

private const val BLUR_KERNEL_SIGMA_OUTSET = 3f
internal const val OPAQUE_LAYER_ALPHA = 255
private const val APPBAR_MASK_TRANSITION_DURATION_MS = 100L
private const val BLUR_TRANSITION_DURATION_MS = 500
private const val APPBAR_SURFACE_TINT_ALPHA = 0.3f
private const val APPBAR_DARK_MASK_ALPHA = 0.12f
private const val APPBAR_PROGRESSIVE_CURVE = 2.4f

private const val PROGRESSIVE_LEVEL_MASK_SHADER = """
uniform shader child;
uniform float2 in_gradBand;
uniform float in_curve;
uniform float in_level;
uniform float in_slope;

half4 main(float2 coord) {
  float raw = clamp((coord.y - in_gradBand.x) / (in_gradBand.y - in_gradBand.x), 0.0, 1.0);
  raw = pow(raw * raw * (3.0 - 2.0 * raw), in_curve);
  float weight = clamp(in_level - raw * in_slope, 0.0, 1.0);
  weight = weight * weight * (3.0 - 2.0 * weight);
  return child.eval(coord) * half(weight);
}
"""
