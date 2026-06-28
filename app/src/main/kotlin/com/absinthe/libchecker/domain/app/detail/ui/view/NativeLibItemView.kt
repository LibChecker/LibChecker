package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isGone
import androidx.core.view.marginEnd
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.displayWidth
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.rulesbundle.Rule

class NativeLibItemView(context: Context) : AViewGroup(context) {

  init {
    isClickable = true
    isFocusable = true
    clipToPadding = false
    val horizontalPadding = context.getDimensionPixelSize(R.dimen.normal_padding)
    val verticalPadding = 4.dp
    setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
    setWillNotDraw(false)
  }

  val libName =
    AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerifMedium)).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).also {
        it.marginEnd = context.getDimensionPixelSize(R.dimen.normal_padding)
      }
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
      addView(this)
    }

  val libSize =
    AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerifCondensed)).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setPadding(0, 1.dp, 0, 1.dp)
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
      setLineSpacing(2.dp.toFloat(), 1.2f)
      addView(this)
    }

  private val chip =
    RuleChipView(context).apply {
      layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 48.dp)
      maxWidth = (context.displayWidth * 0.45f).toInt()
      isGone = true
      addView(this)
    }
  private var chipRule: Rule? = null
  private var chipColorfulIcon: Boolean? = null

  fun setChip(rule: Rule?, colorfulIcon: Boolean) {
    if (chipRule == rule && chipColorfulIcon == colorfulIcon) {
      return
    }
    chipRule = rule
    chipColorfulIcon = colorfulIcon
    if (rule != null) {
      chip.bind(rule, colorfulIcon)
      chip.isGone = false
    } else {
      chip.clear()
      chip.isGone = true
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    val chipWidth = if (chip.isGone) {
      0
    } else {
      chip.autoMeasure()
      chip.measuredWidth + libName.marginEnd
    }
    val textWidth =
      (measuredWidth - paddingStart - paddingEnd - libName.marginEnd - chipWidth).coerceAtLeast(0)

    libName.measure(textWidth.toAtMostMeasureSpec(), libName.defaultHeightMeasureSpec(this))
    libSize.measure(textWidth.toAtMostMeasureSpec(), libSize.defaultHeightMeasureSpec(this))

    setMeasuredDimension(
      measuredWidth,
      (libName.measuredHeight + libSize.measuredHeight + paddingTop + paddingBottom).coerceAtLeast(
        40.dp
      )
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    libName.layout(paddingStart, paddingTop)
    libSize.layout(paddingStart, libName.bottom)
    if (!chip.isGone) {
      chip.layout(paddingEnd, chip.toVerticalCenter(this), fromRight = true)
    }
  }

  var processLabelColor: Int = -1
    set(value) {
      field = value
      paint.color = value
    }

  private val paint = Paint().also {
    it.color = UiUtils.getRandomColor()
    it.style = Paint.Style.FILL
    it.isAntiAlias = true
  }

  private val topCornerStart = PointF()
  private val topCornerControl = PointF()
  private val topCornerEnd = PointF()
  private val bottomCornerStart = PointF()
  private val bottomCornerControl = PointF()
  private val bottomCornerEnd = PointF()
  private val path = Path()

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    topCornerStart.set(0f, 0f)
    topCornerControl.set(5.dp.toFloat(), 1.dp.toFloat())
    topCornerEnd.set(5.dp.toFloat(), 5.dp.toFloat())

    bottomCornerEnd.set(5.dp.toFloat(), h - 5.dp.toFloat())
    bottomCornerControl.set(5.dp.toFloat(), h - 1.dp.toFloat())
    bottomCornerStart.set(0f, h.toFloat())
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    if (processLabelColor != -1) {
      path.apply {
        reset()
        moveTo(topCornerStart.x, topCornerStart.y)
        quadTo(
          topCornerControl.x,
          topCornerControl.y,
          topCornerEnd.x,
          topCornerEnd.y
        )
        lineTo(bottomCornerEnd.x, bottomCornerEnd.y)
        quadTo(
          bottomCornerControl.x,
          bottomCornerControl.y,
          bottomCornerStart.x,
          bottomCornerStart.y
        )
        close()
      }
      canvas.drawPath(path, paint)
    }
  }
}
