/*
 * Copyright 2018 Google LLC
 * Copyright 2018 markushi
 * Copyright 2018 rom4ek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.absinthe.libchecker.view.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.SoundEffectConstants
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.AnimationUtils
import android.widget.Checkable
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.core.animation.doOnEnd
import androidx.core.content.res.getColorOrThrow
import androidx.core.content.res.getDimensionOrThrow
import androidx.core.content.res.getDimensionPixelSizeOrThrow
import androidx.core.content.res.getDrawableOrThrow
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.withScale
import androidx.core.graphics.withTranslation
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.lerp
import com.absinthe.libchecker.utils.extensions.textWidth
import java.text.Bidi
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty

/**
 * A custom view for displaying filters. Allows a custom presentation of the tag color and selection
 * state.
 */
class CheckableChipView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr),
  Checkable {

  companion object {
    private const val CHECKING_DURATION = 350L // ms
    private const val UNCHECKING_DURATION = 200L // ms
  }

  /**
   * Sets the indicator and background color when the widget is checked.
   */
  var checkedColor: Int by viewProperty(0) { indicatorPaint.color = it }

  /**
   * Sets the text color to be used when the widget is not checked.
   */
  var defaultTextColor: Int by viewProperty(0) { textPaint.color = it }

  /**
   * Sets the text color to be used when the widget is checked.
   */
  var checkedTextColor: Int by viewProperty(0)

  /**
   * Sets the text to be displayed.
   */
  var text: CharSequence by viewProperty("", requestLayout = true)

  /**
   * Sets the textSize to be displayed.
   */
  var textSize: Float by viewProperty(0f, requestLayout = true) { textPaint.textSize = it }

  /**
   * Controls the color of the outline.
   */
  var outlineColor: Int by viewProperty(0) { outlinePaint.color = it }

  /**
   * Controls the stroke width of the outline.
   */
  var outlineWidth: Float by viewProperty(0f) { outlinePaint.strokeWidth = it }

  /**
   * Controls the corner radius of the outline. If null the outline will be pill-shaped.
   */
  var outlineCornerRadius: Float? by viewProperty(null)

  /**
   * Sets the listener to be called when the checked state changes.
   */
  var onCheckedChangeListener: ((view: CheckableChipView, checked: Boolean) -> Unit)? = null

  var textColorPair = Color.BLACK to Color.WHITE

  private var targetProgress: Float = 0f

  private var progress: Float by viewProperty(0f) {
    if (it == 0f || it == 1f) {
      onCheckedChangeListener?.invoke(this, isChecked)
    }
  }

  private var padding: Int = 0
  private val outlinePaint: Paint =
    TextPaint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
  private val textPaint: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
  private val indicatorPaint: Paint = TextPaint(Paint.ANTI_ALIAS_FLAG)

  private lateinit var clearDrawable: Drawable
  private lateinit var touchFeedbackDrawable: Drawable

  private lateinit var textLayout: StaticLayout

  private var maxTextWidth: Int = 0

  private val progressAnimator: ValueAnimator by lazy {
    ValueAnimator.ofFloat().apply {
      interpolator =
        AnimationUtils.loadInterpolator(context, android.R.interpolator.fast_out_slow_in)
    }
  }

  init {
    clipToOutline = true
    isClickable = true

    context.withStyledAttributes(
      set = attrs,
      attrs = R.styleable.CheckableChipView,
      defStyleAttr = R.attr.checkableChipViewStyle,
      defStyleRes = R.style.Widget_CheckableChipView
    ) {
      outlineColor = getColorOrThrow(R.styleable.CheckableChipView_ccv_outlineColor)
      outlineWidth = getDimensionOrThrow(R.styleable.CheckableChipView_ccv_outlineWidth)
      if (hasValue(R.styleable.CheckableChipView_ccv_outlineCornerRadius)) {
        outlineCornerRadius =
          getDimensionOrThrow(R.styleable.CheckableChipView_ccv_outlineCornerRadius)
      }

      checkedColor = getColor(R.styleable.CheckableChipView_android_color, checkedColor)
      checkedTextColor =
        getColor(R.styleable.CheckableChipView_ccv_checkedTextColor, Color.TRANSPARENT)
      defaultTextColor = getColorOrThrow(R.styleable.CheckableChipView_android_textColor)

      getString(R.styleable.CheckableChipView_android_text)?.let { text = it }
      textSize =
        getDimension(R.styleable.CheckableChipView_android_textSize, TextView(context).textSize)

      clearDrawable = getDrawableOrThrow(R.styleable.CheckableChipView_ccv_clearIcon).apply {
        setBounds(
          -intrinsicWidth / 2,
          -intrinsicHeight / 2,
          intrinsicWidth / 2,
          intrinsicHeight / 2
        )
      }
      touchFeedbackDrawable =
        getDrawableOrThrow(R.styleable.CheckableChipView_ccv_foreground).apply {
          callback = this@CheckableChipView
        }
      padding = getDimensionPixelSizeOrThrow(R.styleable.CheckableChipView_android_padding)
      isChecked = getBoolean(R.styleable.CheckableChipView_android_checked, false)
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val widthMode = MeasureSpec.getMode(widthMeasureSpec)
    val heightMode = MeasureSpec.getMode(heightMeasureSpec)

    // width
    val nonTextWidth =
      (4 * padding) + (2 * outlinePaint.strokeWidth).toInt() + clearDrawable.intrinsicWidth
    val availableTextWidth = when (widthMode) {
      MeasureSpec.EXACTLY -> MeasureSpec.getSize(widthMeasureSpec) - nonTextWidth
      MeasureSpec.AT_MOST -> MeasureSpec.getSize(widthMeasureSpec) - nonTextWidth
      MeasureSpec.UNSPECIFIED -> Int.MAX_VALUE
      else -> Int.MAX_VALUE
    }
    if (availableTextWidth > maxTextWidth && availableTextWidth < Int.MAX_VALUE) {
      maxTextWidth = availableTextWidth
    }
    createLayout(maxTextWidth)
    val desiredWidth = nonTextWidth + textLayout.textWidth()
    val width = when (widthMode) {
      MeasureSpec.EXACTLY -> MeasureSpec.getSize(widthMeasureSpec)
      MeasureSpec.AT_MOST -> MeasureSpec.getSize(widthMeasureSpec).coerceAtMost(desiredWidth)
      MeasureSpec.UNSPECIFIED -> desiredWidth
      else -> desiredWidth
    }

    // height
    val desiredHeight = padding + textLayout.height + padding
    val height = when (heightMode) {
      MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
      MeasureSpec.AT_MOST -> MeasureSpec.getSize(heightMeasureSpec).coerceAtMost(desiredHeight)
      MeasureSpec.UNSPECIFIED -> desiredHeight
      else -> desiredHeight
    }

    setMeasuredDimension(width, height)
    outlineProvider = object : ViewOutlineProvider() {
      override fun getOutline(view: View, outline: Outline) {
        outline.setRoundRect(0, 0, width, height, outlineCornerRadius ?: (height / 2f))
      }
    }
    touchFeedbackDrawable.setBounds(0, 0, width, height)
  }

  @CallSuper
  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)

    outlinePaint.apply {
      strokeWidth = outlineWidth
      color = outlineColor
    }
    val iconRadius = clearDrawable.intrinsicWidth / 2f
    val halfStroke = outlineWidth / 2f
    val rounding = outlineCornerRadius ?: ((height - outlineWidth) / 2f)

    // Outline
    if (progress < 1f) {
      canvas.drawRoundRect(
        halfStroke,
        halfStroke,
        width - halfStroke,
        height - halfStroke,
        rounding,
        rounding,
        outlinePaint
      )
    }

    val isRtl = layoutDirection == LAYOUT_DIRECTION_RTL

    // Draws beyond bounds and relies on clipToOutline to enforce shape
    val initialIndicatorSize = 8.dp.toFloat()
    val indicatorCenterX = if (isRtl) {
      width - (outlineWidth + padding + padding / 2f + initialIndicatorSize / 2f)
    } else {
      outlineWidth + padding + padding / 2f + initialIndicatorSize / 2f
    }
    val indicatorCenterY = height / 2f

    val indicatorCenterToFarEdge = if (isRtl) {
      indicatorCenterX
    } else {
      width - indicatorCenterX
    }
    val indicatorSize = lerp(
      initialIndicatorSize,
      (indicatorCenterToFarEdge * 2f).coerceAtLeast((height - indicatorCenterY) * 2f),
      progress
    )

    val indicatorSizeHalf = indicatorSize / 2f

    val indicatorRounding = (rounding / (height - outlineWidth)) * (indicatorSizeHalf * 2f)
    indicatorPaint.color = checkedColor

    canvas.drawRoundRect(
      indicatorCenterX - indicatorSizeHalf,
      indicatorCenterY - indicatorSizeHalf,
      indicatorCenterX + indicatorSizeHalf,
      indicatorCenterY + indicatorSizeHalf,
      indicatorRounding,
      indicatorRounding,
      indicatorPaint
    )

    // Text
    val textX = if (isRtl) {
      lerp(
        width - (indicatorCenterX + initialIndicatorSize / 2f) + padding,
        width - (outlineWidth + padding + padding / 2f) - textLayout.textWidth(),
        progress
      )
    } else {
      lerp(
        indicatorCenterX + initialIndicatorSize / 2f + padding,
        outlineWidth + padding + padding / 2f,
        progress
      )
    }

    textPaint.apply {
      textSize = this@CheckableChipView.textSize
      color = when (checkedTextColor) {
        0 -> defaultTextColor
        else -> ColorUtils.blendARGB(defaultTextColor, checkedTextColor, progress)
      }
    }

    canvas.withTranslation(
      x = textX,
      y = (height - textLayout.height) / 2f
    ) {
      textLayout.draw(this)
    }

    // Clear icon
    val iconX = if (isRtl) {
      outlineWidth + padding + iconRadius
    } else {
      width - outlineWidth - padding - iconRadius
    }

    if (progress > 0f) {
      canvas.withTranslation(
        x = iconX,
        y = height / 2f
      ) {
        canvas.withScale(progress, progress) {
          clearDrawable.draw(canvas)
        }
      }
    }

    // Touch feedback
    touchFeedbackDrawable.draw(canvas)
  }

  /**
   * Starts the animation to enable/disable a filter and invokes a function when done.
   */
  fun setCheckedAnimated(checked: Boolean, onEnd: (() -> Unit)?) {
    targetProgress = if (checked) 1f else 0f
    if (targetProgress != progress) {
      progressAnimator.apply {
        removeAllListeners()
        cancel()
        setFloatValues(progress, targetProgress)
        duration = if (checked) CHECKING_DURATION else UNCHECKING_DURATION
        addUpdateListener {
          progress = it.animatedValue as Float
        }
        doOnEnd {
          progress = targetProgress
          onEnd?.invoke()
        }
        start()
      }
    }
    checkedTextColor = if (checked) {
      textColorPair.first
    } else {
      textColorPair.second
    }
  }

  override fun performClick(): Boolean {
    setCheckedAnimated(!isChecked, null)
    val handled = super.performClick()
    if (!handled) {
      playSoundEffect(SoundEffectConstants.CLICK)
    }
    return handled
  }

  override fun isChecked() = targetProgress == 1f

  override fun toggle() {
    isChecked = !isChecked
  }

  override fun setChecked(checked: Boolean) {
    targetProgress = if (checked) 1f else 0f
    progress = targetProgress
    checkedTextColor = if (checked) {
      textColorPair.first
    } else {
      textColorPair.second
    }
  }

  private fun createLayout(textWidth: Int) {
    val alignment = if (Bidi(text.toString(), Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT).isRightToLeft) {
      Layout.Alignment.ALIGN_OPPOSITE
    } else {
      Layout.Alignment.ALIGN_NORMAL
    }
    textLayout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, textWidth)
      .setAlignment(alignment)
      .build()
  }

  override fun verifyDrawable(who: Drawable): Boolean {
    return super.verifyDrawable(who) || who == touchFeedbackDrawable
  }

  override fun drawableStateChanged() {
    super.drawableStateChanged()
    touchFeedbackDrawable.state = drawableState
  }

  override fun jumpDrawablesToCurrentState() {
    super.jumpDrawablesToCurrentState()
    touchFeedbackDrawable.jumpToCurrentState()
  }

  override fun drawableHotspotChanged(x: Float, y: Float) {
    super.drawableHotspotChanged(x, y)
    touchFeedbackDrawable.setHotspot(x, y)
  }

  override fun onSaveInstanceState(): Parcelable {
    return SavedState(super.onSaveInstanceState()!!).apply {
      checked = isChecked
    }
  }

  override fun onRestoreInstanceState(state: Parcelable?) {
    val savedState = state as SavedState
    super.onRestoreInstanceState(savedState.superState)

    // prevent listener from being invoked while restoring the state
    val listener = onCheckedChangeListener
    onCheckedChangeListener = null
    isChecked = savedState.checked
    onCheckedChangeListener = listener
  }

  class SavedState : BaseSavedState {

    @Suppress("unused")
    companion object {
      @JvmField
      val CREATOR = object : Parcelable.Creator<SavedState> {
        override fun createFromParcel(source: Parcel): SavedState = SavedState(source)
        override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
      }
    }

    var checked: Boolean = false

    constructor(state: Parcelable) : super(state)

    constructor(parcel: Parcel) : super(parcel) {
      checked = parcel.readInt() == 1
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
      super.writeToParcel(dest, flags)
      dest.writeInt(if (checked) 1 else 0)
    }

    override fun describeContents() = 0
  }

  /**
   * Observable delegate, which prevents view property from setting the same value and after that requests layout if [requestLayout] is true,
   * or calls [postInvalidateOnAnimation] otherwise.
   *
   * @param default default initial value for a view property
   * @param requestLayout defines whether this view calls [requestLayout] or [postInvalidateOnAnimation] after changing a view property
   * @param afterChangeActions custom actions to be performed after changing a view property
   */
  private fun <T> viewProperty(
    default: T,
    requestLayout: Boolean = false,
    afterChangeActions: ((newValue: T) -> Unit)? = null
  ) = object : ObservableProperty<T>(default) {

    override fun beforeChange(property: KProperty<*>, oldValue: T, newValue: T): Boolean =
      newValue != oldValue

    override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) {
      afterChangeActions?.invoke(newValue)
      if (requestLayout) {
        requestLayout()
      } else {
        postInvalidateOnAnimation()
      }
    }
  }
}
