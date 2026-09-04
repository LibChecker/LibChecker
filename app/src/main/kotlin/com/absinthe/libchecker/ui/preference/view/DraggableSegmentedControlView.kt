package com.absinthe.libchecker.ui.preference.view

import android.content.Context
import android.content.res.ColorStateList
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.TextViewCompat
import com.absinthe.libchecker.ui.preference.model.PreferenceInlineControl
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.view.drawable.G2PillDrawable
import com.google.android.material.R as MaterialR
import kotlin.math.abs

class DraggableSegmentedControlView private constructor(
  context: Context,
  private val segments: List<Segment>,
  private val values: List<String>,
  selectedValue: String?,
  private val deferSelectionUntilAnimationEnd: Boolean,
  private val onChoiceSelected: (String) -> Unit
) : FrameLayout(context) {

  constructor(
    context: Context,
    control: PreferenceInlineControl.IconSegmentedChoice,
    onChoiceSelected: (String) -> Unit
  ) : this(
    context = context,
    segments = control.iconResIds.mapIndexed { index, iconResId ->
      Segment(
        accessibilityLabel = control.accessibilityLabels[index],
        iconResId = iconResId
      )
    },
    values = control.entryValues,
    selectedValue = control.selectedValue,
    deferSelectionUntilAnimationEnd = control.deferSelectionUntilAnimationEnd,
    onChoiceSelected = onChoiceSelected
  )

  constructor(
    context: Context,
    control: PreferenceInlineControl.DraggableChoice,
    onChoiceSelected: (String) -> Unit
  ) : this(
    context = context,
    segments = control.entries.map {
      Segment(accessibilityLabel = it, text = it)
    },
    values = control.entryValues,
    selectedValue = control.selectedValue,
    deferSelectionUntilAnimationEnd = control.deferSelectionUntilAnimationEnd,
    onChoiceSelected = onChoiceSelected
  )

  private val preferredSegmentWidth = (
    if (segments.any { it.text != null }) {
      TEXT_SEGMENT_WIDTH_DP
    } else {
      ICON_SEGMENT_WIDTH_DP
    }
    ).dp
  private var segmentWidth = preferredSegmentWidth
  private val groupHeight = GROUP_HEIGHT_DP.dp
  private val thumbHeight = THUMB_HEIGHT_DP.dp
  private val groupInset = GROUP_INSET_DP.dp
  private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
  private val thumb = FrameLayout(context)
  private val buttonContainer = LinearLayout(context)
  private val buttons = mutableListOf<View>()
  private var hasValidSelectedValue = selectedValue in values
  private var selectedIndex = selectedValue
    ?.let(values::indexOf)
    ?.takeIf { it >= 0 }
    ?: 0
  private var pendingIndex = selectedIndex
  private var downX = 0f
  private var downY = 0f
  private var dragging = false
  private var thumbAnimationId = 0L
  private var pendingSelectionCompletion: (() -> Unit)? = null

  init {
    isClickable = true
    clipToOutline = true
    background = pillDrawable(
      fillColor = context.getColorByAttr(MaterialR.attr.colorSurfaceContainerHighest),
      strokeColor = context.getColorByAttr(MaterialR.attr.colorOutlineVariant)
    )

    thumb.apply {
      background = pillDrawable(
        fillColor = context.getColorByAttr(MaterialR.attr.colorSecondaryContainer)
      )
      ViewCompat.setElevation(this, THUMB_ELEVATION_DP.dp.toFloat())
      alpha = if (hasValidSelectedValue) 1f else 0f
    }
    addView(
      thumb,
      LayoutParams(segmentWidth, thumbHeight, Gravity.START or Gravity.CENTER_VERTICAL).apply {
        marginStart = groupInset
      }
    )

    buttonContainer.apply {
      gravity = Gravity.CENTER_VERTICAL
      orientation = LinearLayout.HORIZONTAL
      setPadding(groupInset, 0, groupInset, 0)
      ViewCompat.setElevation(this, CONTENT_LAYER_ELEVATION_DP.dp.toFloat())
    }
    addView(
      buttonContainer,
      LayoutParams(
        groupInset * 2 + segmentWidth * values.size,
        groupHeight
      )
    )

    segments.forEachIndexed { index, segment ->
      val button = segment.buildView(context).apply {
        setOnClickListener {
          selectIndex(index, notify = true)
        }
      }
      buttons += button
      buttonContainer.addView(
        button,
        LinearLayout.LayoutParams(segmentWidth, groupHeight)
      )
    }
    thumb.translationX = translationForIndex(selectedIndex)
    updateButtonStates(if (hasValidSelectedValue) selectedIndex else NO_SELECTED_INDEX)
  }

  override fun onMeasure(
    widthMeasureSpec: Int,
    heightMeasureSpec: Int
  ) {
    val availableWidth = when (MeasureSpec.getMode(widthMeasureSpec)) {
      MeasureSpec.UNSPECIFIED -> groupInset * 2 + preferredSegmentWidth * values.size
      else -> MeasureSpec.getSize(widthMeasureSpec)
    }
    updateSegmentWidth(
      calculateSegmentWidth(
        preferredWidth = preferredSegmentWidth,
        segmentCount = values.size,
        groupInset = groupInset,
        availableWidth = availableWidth
      )
    )
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
  }

  override fun onRtlPropertiesChanged(layoutDirection: Int) {
    super.onRtlPropertiesChanged(layoutDirection)
    if (!dragging) {
      thumb.translationX = translationForIndex(pendingIndex)
    }
  }

  override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        downX = event.x
        downY = event.y
        dragging = false
        if (pendingSelectionCompletion == null) {
          pendingIndex = selectedIndex
        }
      }

      MotionEvent.ACTION_MOVE -> {
        val deltaX = event.x - downX
        val deltaY = event.y - downY
        if (abs(deltaX) > touchSlop && abs(deltaX) > abs(deltaY)) {
          dragging = true
          parent?.requestDisallowInterceptTouchEvent(true)
          return true
        }
      }
    }
    return false
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    when (event.actionMasked) {
      MotionEvent.ACTION_MOVE -> {
        if (dragging) {
          updateDrag(event.x)
        }
      }

      MotionEvent.ACTION_UP -> {
        if (dragging) {
          updateDrag(event.x)
          finishDrag(commit = true)
        }
      }

      MotionEvent.ACTION_CANCEL -> {
        if (dragging) {
          finishDrag(commit = false)
        }
      }
    }
    return true
  }

  override fun onDetachedFromWindow() {
    val pendingCompletion = pendingSelectionCompletion
    cancelThumbAnimation()
    pendingCompletion?.invoke()
    super.onDetachedFromWindow()
  }

  private fun updateDrag(pointerX: Float) {
    cancelThumbAnimation()
    thumb.alpha = 1f
    val isRtl = isRtl()
    val logicalTranslation = logicalSegmentTranslationForPointer(
      pointerX = pointerX,
      viewWidth = width,
      segmentWidth = segmentWidth,
      segmentCount = values.size,
      groupInset = groupInset,
      isRtl = isRtl
    )
    thumb.translationX = if (isRtl) -logicalTranslation else logicalTranslation

    val nextIndex = segmentIndexForPointer(
      pointerX = pointerX,
      viewWidth = width,
      segmentWidth = segmentWidth,
      segmentCount = values.size,
      groupInset = groupInset,
      isRtl = isRtl
    )
    if (nextIndex != pendingIndex) {
      pendingIndex = nextIndex
      performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
      updateButtonStates(pendingIndex)
    }
  }

  private fun finishDrag(commit: Boolean) {
    val targetIndex = if (commit) pendingIndex else selectedIndex
    dragging = false
    parent?.requestDisallowInterceptTouchEvent(false)
    if (
      commit &&
      shouldNotifySegmentSelection(
        selectedIndex = selectedIndex,
        requestedIndex = targetIndex,
        hasValidSelectedValue = hasValidSelectedValue
      )
    ) {
      selectIndex(targetIndex, notify = true)
    } else {
      pendingIndex = selectedIndex
      updateButtonStates(if (hasValidSelectedValue) selectedIndex else NO_SELECTED_INDEX)
      animateThumbTo(selectedIndex)
      if (!hasValidSelectedValue) {
        thumb.animate().alpha(0f)
      }
    }
  }

  private fun selectIndex(
    index: Int,
    notify: Boolean
  ) {
    if (index !in values.indices) {
      return
    }
    if (
      deferSelectionUntilAnimationEnd &&
      pendingSelectionCompletion != null &&
      pendingIndex == index
    ) {
      return
    }
    pendingIndex = index
    thumb.alpha = 1f
    updateButtonStates(index)
    val shouldNotify = notify && shouldNotifySegmentSelection(
      selectedIndex = selectedIndex,
      requestedIndex = index,
      hasValidSelectedValue = hasValidSelectedValue
    )
    if (shouldNotify) {
      performHapticFeedback(HapticFeedbackConstants.CONFIRM)
      if (deferSelectionUntilAnimationEnd) {
        animateThumbTo(index) {
          commitSelection(index)
        }
      } else {
        commitSelection(index)
        animateThumbTo(index)
      }
    } else {
      animateThumbTo(index)
    }
  }

  private fun animateThumbTo(
    index: Int,
    onAnimationFinished: (() -> Unit)? = null
  ) {
    cancelThumbAnimation()
    val currentAnimationId = thumbAnimationId
    pendingSelectionCompletion = onAnimationFinished
    thumb.animate()
      .translationX(translationForIndex(index))
      .setDuration(THUMB_ANIMATION_DURATION_MS)
      .setInterpolator(THUMB_INTERPOLATOR)
      .withEndAction {
        if (currentAnimationId == thumbAnimationId) {
          val completion = pendingSelectionCompletion
          pendingSelectionCompletion = null
          completion?.invoke()
        }
      }
      .start()
  }

  private fun cancelThumbAnimation() {
    thumbAnimationId += 1
    thumb.animate().cancel()
    pendingSelectionCompletion = null
  }

  private fun commitSelection(index: Int) {
    selectedIndex = index
    pendingIndex = index
    hasValidSelectedValue = true
    onChoiceSelected(values[index])
  }

  private fun updateSegmentWidth(newWidth: Int) {
    if (newWidth == segmentWidth) {
      return
    }
    segmentWidth = newWidth
    thumb.layoutParams = thumb.layoutParams.apply {
      width = segmentWidth
    }
    buttonContainer.layoutParams = buttonContainer.layoutParams.apply {
      width = groupInset * 2 + segmentWidth * values.size
    }
    buttons.forEach { button ->
      button.layoutParams = button.layoutParams.apply {
        width = segmentWidth
      }
    }
    if (!dragging) {
      thumb.translationX = translationForIndex(pendingIndex)
    }
  }

  private fun translationForIndex(index: Int): Float {
    return segmentTranslation(
      index = index,
      segmentWidth = segmentWidth,
      isRtl = isRtl()
    )
  }

  private fun isRtl(): Boolean {
    return layoutDirection == View.LAYOUT_DIRECTION_RTL
  }

  private fun updateButtonStates(activeIndex: Int) {
    val selectedColor = context.getColorByAttr(MaterialR.attr.colorOnSecondaryContainer)
    val unselectedColor = context.getColorByAttr(MaterialR.attr.colorOnSurfaceVariant)
    buttons.forEachIndexed { index, button ->
      val selected = index == activeIndex
      button.isSelected = selected
      val color = if (selected) selectedColor else unselectedColor
      when (button) {
        is AppCompatImageButton -> {
          ImageViewCompat.setImageTintList(button, ColorStateList.valueOf(color))
        }

        is AppCompatTextView -> button.setTextColor(color)
      }
    }
  }

  private fun Segment.buildView(context: Context): View {
    val iconResId = iconResId
    if (iconResId != null) {
      return AppCompatImageButton(context).apply {
        background = null
        contentDescription = accessibilityLabel
        setImageResource(iconResId)
        setPadding(ICON_PADDING_DP.dp, ICON_PADDING_DP.dp, ICON_PADDING_DP.dp, ICON_PADDING_DP.dp)
        TooltipCompat.setTooltipText(this, accessibilityLabel)
      }
    }

    return AppCompatTextView(context).apply {
      background = null
      gravity = Gravity.CENTER
      includeFontPadding = false
      maxLines = 1
      ellipsize = TextUtils.TruncateAt.END
      text = this@buildView.text
      setTextAppearance(MaterialR.style.TextAppearance_Material3_LabelLarge)
      contentDescription = accessibilityLabel
      TooltipCompat.setTooltipText(this, accessibilityLabel)
      TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
        this,
        TEXT_MIN_SIZE_SP,
        TEXT_MAX_SIZE_SP,
        TEXT_SIZE_STEP_SP,
        TypedValue.COMPLEX_UNIT_SP
      )
    }
  }

  private fun pillDrawable(
    fillColor: Int,
    strokeColor: Int? = null
  ): G2PillDrawable {
    return G2PillDrawable(
      fillColor = fillColor,
      strokeColor = strokeColor,
      strokeWidth = if (strokeColor == null) 0f else STROKE_WIDTH_DP.dp.toFloat()
    )
  }

  private companion object {
    const val GROUP_HEIGHT_DP = 48
    const val GROUP_INSET_DP = 4
    const val ICON_SEGMENT_WIDTH_DP = 48
    const val TEXT_SEGMENT_WIDTH_DP = 80
    const val TEXT_MIN_SIZE_SP = 10
    const val TEXT_MAX_SIZE_SP = 14
    const val TEXT_SIZE_STEP_SP = 1
    const val THUMB_HEIGHT_DP = 40
    const val THUMB_ELEVATION_DP = 1
    const val CONTENT_LAYER_ELEVATION_DP = 2
    const val ICON_PADDING_DP = 12
    const val STROKE_WIDTH_DP = 1
    const val THUMB_ANIMATION_DURATION_MS = 250L
    const val NO_SELECTED_INDEX = -1
    val THUMB_INTERPOLATOR = PathInterpolator(0.22f, 1f, 0.36f, 1f)
  }

  private data class Segment(
    val accessibilityLabel: String,
    val iconResId: Int? = null,
    val text: String? = null
  )
}
