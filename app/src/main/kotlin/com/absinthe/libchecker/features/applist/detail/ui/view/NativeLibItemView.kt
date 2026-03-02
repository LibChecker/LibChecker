package com.absinthe.libchecker.features.applist.detail.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.text.TextUtils
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.children
import androidx.core.view.marginEnd
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.displayWidth
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.rulesbundle.Rule
import com.google.android.material.chip.Chip

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

  private var chip: Chip? = null

  fun setChip(rule: Rule?) {
    chip = rule?.let {
      getOrCreateChip().apply {
        text = it.label
        setChipIconResource(it.iconRes)

        if (!GlobalValues.isColorfulIcon && !it.isSimpleColorIcon) {
          chipIcon?.let { icon ->
            icon.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
            chipIcon = icon
          }
        }
      }
    } ?: run {
      chip?.let { removeView(it) }
      null
    }
  }

  private fun getOrCreateChip() = chip ?: Chip(context).apply {
    isClickable = false
    layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 48.dp)
    maxWidth = (context.displayWidth * 0.45f).toInt()
    ellipsize = TextUtils.TruncateAt.MIDDLE
    addView(this)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    val chipWidth = chip?.let {
      it.autoMeasure()
      return@let it.measuredWidth + libName.marginEnd
    } ?: 0
    val maxWidth = measuredWidth - paddingStart - paddingEnd - libName.marginEnd - chipWidth

    children.filter { it != chip }.forEach {
      it.autoMeasure()
      if (it.measuredWidth > maxWidth) {
        it.measure(maxWidth.toExactlyMeasureSpec(), it.defaultHeightMeasureSpec(this))
      }
    }

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
    chip?.let { it.layout(paddingEnd, it.toVerticalCenter(this), fromRight = true) }
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
