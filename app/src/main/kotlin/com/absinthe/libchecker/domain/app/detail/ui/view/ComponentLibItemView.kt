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
import androidx.core.view.marginEnd
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.LibStringComponentItemDisplay
import com.absinthe.libchecker.domain.app.detail.model.LibStringPermissionItemDisplay
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize

class ComponentLibItemView(context: Context) : RuleChipItemView(context) {

  init {
    setWillNotDraw(false)
  }

  private val libName =
    AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerifMedium)).apply {
      id = android.R.id.title
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

  fun bind(
    display: LibStringComponentItemDisplay,
    highlightText: String,
    colorfulRuleIcon: Boolean
  ) {
    libName.setLibStringItemName(display.name, highlightText)
    bindRuleChip(display.rule, colorfulRuleIcon)
    processLabelColor = display.processIndicatorColor ?: -1
    contentDescription = display.contentDescription
  }

  fun bind(
    display: LibStringPermissionItemDisplay,
    highlightText: String
  ) {
    libName.setLibStringItemName(display.name, highlightText)
    bindRuleChip(null, false)
    processLabelColor = if (display.showNotGrantedIndicator) {
      R.color.material_red_500.getColor(context)
    } else {
      -1
    }
    contentDescription = display.contentDescription
  }

  private var shouldBreakLines = false

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    var chipWidth = ruleChip?.apply { autoMeasure() }?.measuredWidth ?: 0

    if (chipWidth > (measuredWidth * 4 / 7)) {
      chipWidth = 0
      shouldBreakLines = true
    } else {
      shouldBreakLines = false
    }

    val libNameWidth = measuredWidth - paddingStart - paddingEnd - libName.marginEnd - chipWidth
    libName.autoMeasure()
    if (libName.measuredWidth > libNameWidth) {
      libName.measure(libNameWidth.toExactlyMeasureSpec(), libName.defaultHeightMeasureSpec(this))
    }
    val height = if (shouldBreakLines) {
      libName.measuredHeight + paddingTop + paddingBottom + (ruleChip?.measuredHeight ?: 0)
    } else {
      libName.measuredHeight + paddingTop + paddingBottom
    }.coerceAtLeast(40.dp)
    setMeasuredDimension(measuredWidth, height)
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    if (shouldBreakLines) {
      libName.layout(paddingStart, paddingTop)
      ruleChip?.layout(paddingStart, libName.bottom)
    } else {
      libName.layout(paddingStart, libName.toVerticalCenter(this))
      ruleChip?.let {
        it.layout(paddingEnd, it.toVerticalCenter(this), fromRight = true)
      }
    }
  }

  private var processLabelColor: Int = -1
    set(value) {
      field = value
      paint.color = value
      invalidate()
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
