package com.absinthe.libchecker.ui.preference.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.absinthe.libchecker.R
import com.absinthe.libchecker.ui.preference.model.PreferenceInlineControl
import com.absinthe.libchecker.ui.preference.model.PreferenceItemRenderState
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.card.MaterialCardView

class PreferenceItemView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

  private val iconFrame by lazy { findViewById<FrameLayout>(R.id.icon_frame) }
  private val icon by lazy { findViewById<View>(android.R.id.icon) }
  private val widgetFrame by lazy { findViewById<View>(android.R.id.widget_frame) }
  private val chevron by lazy { findViewById<View>(R.id.settings_preference_chevron) }
  private val title by lazy { findViewById<View>(android.R.id.title) }
  private val summary by lazy { findViewById<View>(android.R.id.summary) }
  private val inlineControl by lazy {
    findViewById<PreferenceInlineControlView>(R.id.settings_preference_inline_control)
  }
  private var badge: BadgeDrawable? = null
  private var shapeAnimator: ValueAnimator? = null
  private var inlineAnimator: ValueAnimator? = null
  private var baseTopRadius = 0f
  private var baseBottomRadius = 0f
  private var currentTopRadius = 0f
  private var currentBottomRadius = 0f
  private var shapeReady = false
  private var renderedPressed = false
  private var renderedExpanded: Boolean? = null
  private var renderedPreferenceKey: String? = null

  override fun onFinishInflate() {
    super.onFinishInflate()
    title.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    summary.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
  }

  override fun drawableStateChanged() {
    super.drawableStateChanged()
    if (!shapeReady || renderedPressed == isPressed) {
      return
    }
    renderedPressed = isPressed
    animateShape(isPressed)
  }

  override fun onDetachedFromWindow() {
    shapeAnimator?.cancel()
    shapeAnimator = null
    inlineAnimator?.cancel()
    inlineAnimator = null
    super.onDetachedFromWindow()
  }

  fun bind(
    state: PreferenceItemRenderState,
    animateExpansion: Boolean = false,
    onChoiceSelected: (String) -> Unit = {},
    onRangeValueChangeFinished: (Int) -> Unit = {}
  ) {
    val isNewPreference = renderedPreferenceKey != state.preferenceKey
    if (isNewPreference) {
      inlineAnimator?.cancel()
      renderedExpanded = null
      renderedPreferenceKey = state.preferenceKey
    }

    bindShape(state, reset = isNewPreference)
    widgetFrame.isVisible = state.toggleChecked != null
    chevron.isVisible = state.showChevron || state.inlineControl != null
    bindInlineControl(
      state = state,
      animateExpansion = animateExpansion && !isNewPreference,
      onChoiceSelected = onChoiceSelected,
      onRangeValueChangeFinished = onRangeValueChangeFinished
    )
    bindBadge(state.badgeDescription != null)
    contentDescription = buildContentDescription(state)
    ViewCompat.setStateDescription(
      this,
      state.inlineControl?.let {
        context.getString(
          if (state.expanded) {
            R.string.a11y_state_expanded
          } else {
            R.string.a11y_state_collapsed
          }
        )
      }
    )
  }

  private fun bindShape(
    state: PreferenceItemRenderState,
    reset: Boolean
  ) {
    val outerRadius = resources.getDimension(R.dimen.settings_preference_corner_radius)
    val innerRadius = resources.getDimension(R.dimen.settings_preference_inner_corner_radius)
    val newTopRadius = if (state.groupPosition.usesOuterTopCorners) outerRadius else innerRadius
    val newBottomRadius = if (state.groupPosition.usesOuterBottomCorners) outerRadius else innerRadius
    val baseShapeChanged =
      newTopRadius != baseTopRadius || newBottomRadius != baseBottomRadius
    baseTopRadius = newTopRadius
    baseBottomRadius = newBottomRadius
    if (reset || !shapeReady || baseShapeChanged) {
      shapeAnimator?.cancel()
      shapeAnimator = null
      renderedPressed = isPressed
      currentTopRadius = if (isPressed) outerRadius else baseTopRadius
      currentBottomRadius = if (isPressed) outerRadius else baseBottomRadius
      shapeReady = true
      applyShape(currentTopRadius, currentBottomRadius)
    } else if (shapeAnimator?.isRunning != true) {
      currentTopRadius = if (isPressed) outerRadius else baseTopRadius
      currentBottomRadius = if (isPressed) outerRadius else baseBottomRadius
      applyShape(currentTopRadius, currentBottomRadius)
    }

    updateLayoutParams<ViewGroup.MarginLayoutParams> {
      topMargin = if (state.groupPosition.usesOuterTopCorners) {
        0
      } else {
        resources.getDimensionPixelSize(R.dimen.settings_preference_card_spacing)
      }
      bottomMargin = 0
    }
  }

  private fun animateShape(pressed: Boolean) {
    val pressedRadius = resources.getDimension(R.dimen.settings_preference_corner_radius)
    val targetTopRadius = if (pressed) pressedRadius else baseTopRadius
    val targetBottomRadius = if (pressed) pressedRadius else baseBottomRadius
    val startTopRadius = currentTopRadius
    val startBottomRadius = currentBottomRadius
    shapeAnimator?.cancel()
    shapeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
      duration = if (pressed) PRESS_SHAPE_DURATION_MS else RELEASE_SHAPE_DURATION_MS
      interpolator = EXPRESSIVE_INTERPOLATOR
      addUpdateListener { animator ->
        val progress = animator.animatedValue as Float
        currentTopRadius = lerp(startTopRadius, targetTopRadius, progress)
        currentBottomRadius = lerp(startBottomRadius, targetBottomRadius, progress)
        applyShape(currentTopRadius, currentBottomRadius)
      }
      start()
    }
  }

  private fun applyShape(
    topRadius: Float,
    bottomRadius: Float
  ) {
    shapeAppearanceModel = shapeAppearanceModel.toBuilder()
      .setTopLeftCornerSize(topRadius)
      .setTopRightCornerSize(topRadius)
      .setBottomLeftCornerSize(bottomRadius)
      .setBottomRightCornerSize(bottomRadius)
      .build()
  }

  private fun bindInlineControl(
    state: PreferenceItemRenderState,
    animateExpansion: Boolean,
    onChoiceSelected: (String) -> Unit,
    onRangeValueChangeFinished: (Int) -> Unit
  ) {
    val control = state.inlineControl
    if (control == null) {
      inlineAnimator?.cancel()
      inlineControl.isVisible = false
      inlineControl.alpha = 1f
      inlineControl.updateLayoutParams { height = ViewGroup.LayoutParams.WRAP_CONTENT }
      renderedExpanded = false
      chevron.rotation = 0f
      return
    }

    inlineControl.bind(
      title = state.title,
      control = control,
      onChoiceSelected = onChoiceSelected,
      onRangeValueChangeFinished = onRangeValueChangeFinished
    )

    val previousExpanded = renderedExpanded
    when {
      previousExpanded == null -> setInlineExpandedImmediately(state.expanded)
      previousExpanded != state.expanded && animateExpansion -> animateInlineExpansion(state.expanded)
      previousExpanded != state.expanded -> setInlineExpandedImmediately(state.expanded)
      inlineAnimator == null -> setInlineExpandedImmediately(state.expanded)
    }
    renderedExpanded = state.expanded
    animateChevron(state.expanded, animateExpansion && previousExpanded != state.expanded)
  }

  private fun setInlineExpandedImmediately(expanded: Boolean) {
    inlineAnimator?.cancel()
    inlineAnimator = null
    inlineControl.isVisible = expanded
    inlineControl.alpha = 1f
    inlineControl.updateLayoutParams { height = ViewGroup.LayoutParams.WRAP_CONTENT }
  }

  private fun animateInlineExpansion(expanded: Boolean) {
    inlineAnimator?.cancel()
    val startHeight = if (inlineControl.isVisible) inlineControl.height else 0
    val startAlpha = if (inlineControl.isVisible) inlineControl.alpha else 0f
    inlineControl.isVisible = true
    inlineControl.updateLayoutParams { height = ViewGroup.LayoutParams.WRAP_CONTENT }
    inlineControl.measure(
      MeasureSpec.makeMeasureSpec(width.coerceAtLeast(1), MeasureSpec.EXACTLY),
      MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
    )
    val measuredHeight = inlineControl.measuredHeight
    val targetHeight = if (expanded) measuredHeight else 0
    val targetAlpha = if (expanded) 1f else 0f
    inlineControl.updateLayoutParams { height = startHeight }
    inlineControl.alpha = startAlpha

    inlineAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
      duration = if (expanded) EXPAND_DURATION_MS else COLLAPSE_DURATION_MS
      interpolator = EXPRESSIVE_INTERPOLATOR
      addUpdateListener { animator ->
        val progress = animator.animatedValue as Float
        inlineControl.updateLayoutParams {
          height = lerp(startHeight.toFloat(), targetHeight.toFloat(), progress).toInt()
        }
        inlineControl.alpha = lerp(startAlpha, targetAlpha, progress)
      }
      addListener(
        object : AnimatorListenerAdapter() {
          private var cancelled = false

          override fun onAnimationCancel(animation: Animator) {
            cancelled = true
          }

          override fun onAnimationEnd(animation: Animator) {
            if (cancelled) {
              return
            }
            inlineAnimator = null
            inlineControl.isVisible = expanded
            inlineControl.alpha = 1f
            inlineControl.updateLayoutParams { height = ViewGroup.LayoutParams.WRAP_CONTENT }
            if (expanded) {
              requestRectangleOnScreen(Rect(0, 0, width, height), false)
            }
          }
        }
      )
      start()
    }
  }

  private fun animateChevron(
    expanded: Boolean,
    animate: Boolean
  ) {
    val targetRotation = if (expanded) 90f else 0f
    chevron.animate().cancel()
    if (!animate) {
      chevron.rotation = targetRotation
      return
    }
    chevron.animate()
      .rotation(targetRotation)
      .setDuration(if (expanded) EXPAND_DURATION_MS else COLLAPSE_DURATION_MS)
      .setInterpolator(EXPRESSIVE_INTERPOLATOR)
      .start()
  }

  @SuppressLint("RestrictedApi")
  private fun bindBadge(visible: Boolean) {
    badge?.let { BadgeUtils.detachBadgeDrawable(it, icon) }
    badge = null
    if (!visible) {
      return
    }

    badge = BadgeDrawable.create(context).apply {
      backgroundColor = context.getColorByAttr(androidx.appcompat.R.attr.colorError)
      badgeGravity = BadgeDrawable.TOP_END
      clearNumber()
    }.also {
      BadgeUtils.attachBadgeDrawable(it, icon, iconFrame)
    }
  }

  private fun buildContentDescription(state: PreferenceItemRenderState): String {
    return buildList {
      state.title?.trim()?.takeIf(String::isNotEmpty)?.let(::add)
      state.summary?.trim()?.takeIf(String::isNotEmpty)?.let(::add)
      state.toggleChecked?.let {
        add(
          context.getString(
            if (it) {
              R.string.array_dark_mode_on
            } else {
              R.string.array_dark_mode_off
            }
          )
        )
      }
      (state.inlineControl as? PreferenceInlineControl.Range)?.value?.let {
        add(it.toString())
      }
      state.badgeDescription?.trim()?.takeIf(String::isNotEmpty)?.let(::add)
    }.joinToString()
  }

  private fun lerp(
    start: Float,
    end: Float,
    progress: Float
  ): Float {
    return start + (end - start) * progress
  }

  private companion object {
    const val PRESS_SHAPE_DURATION_MS = 120L
    const val RELEASE_SHAPE_DURATION_MS = 220L
    const val EXPAND_DURATION_MS = 280L
    const val COLLAPSE_DURATION_MS = 220L
    val EXPRESSIVE_INTERPOLATOR = PathInterpolator(0.2f, 0f, 0f, 1f)
  }
}
