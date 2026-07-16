package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.text.TextUtils
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isGone
import androidx.core.view.marginTop
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.DexFileOptimizationInfo
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getColorStateListByAttr
import com.absinthe.libchecker.utils.extensions.setLongClickCopiedToClipboard
import com.absinthe.libchecker.utils.extensions.setSmoothRoundCorner
import com.absinthe.libchecker.view.AViewGroup
import com.google.android.material.card.MaterialCardView

class AppDexoptItemView(context: Context) : AViewGroup(context) {

  private val titleView = AppCompatTextView(
    ContextThemeWrapper(context, R.style.TextView_SansSerifMedium)
  ).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 8.dp
    }
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
    text = context.getString(R.string.lib_detail_app_dexopt_title)
  }

  private val contentView = ContentView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    isClickable = false
    isLongClickable = false
    isFocusable = false
    isFocusableInTouchMode = false
  }

  private val container = MaterialCardView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 8.dp
    }
    setSmoothRoundCorner(12.dp)
    strokeWidth = 1.dp
    strokeColor = context.getColorByAttr(com.google.android.material.R.attr.colorOutlineVariant)
    setCardBackgroundColor(context.getColorStateListByAttr(com.google.android.material.R.attr.colorSurfaceContainerHigh))
  }

  init {
    addView(titleView)
    container.addView(contentView)
    addView(container)
  }

  fun bind(info: DexFileOptimizationInfo) {
    contentView.bind(info)
    container.setLongClickCopiedToClipboard(contentView.getAllContentText())
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val parent = parent as ViewGroup
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    titleView.autoMeasure()
    container.measure(
      (measuredWidth - parent.paddingStart - parent.paddingEnd).toExactlyMeasureSpec(),
      if (container.isGone) 0 else container.defaultHeightMeasureSpec(parent)
    )
    setMeasuredDimension(
      measuredWidth,
      titleView.marginTop +
        titleView.measuredHeight +
        container.marginTop +
        container.measuredHeight
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    titleView.layout(paddingStart, titleView.marginTop)
    container.layout(paddingStart, titleView.bottom + container.marginTop)
  }

  private class ContentView(context: Context) : AViewGroup(context) {

    private val cardGap = 8.dp

    private val statusCard = DexoptValueCardView(
      context,
      context.getString(R.string.lib_detail_app_dexopt_status)
    ).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
    }

    private val reasonCard = DexoptValueCardView(
      context,
      context.getString(R.string.lib_detail_app_dexopt_reason)
    ).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
    }

    init {
      setPadding(8.dp, 8.dp, 8.dp, 8.dp)
      addView(statusCard)
      addView(reasonCard)
    }

    fun bind(info: DexFileOptimizationInfo) {
      statusCard.setValue(info.status)
      reasonCard.setValue(info.reason)
    }

    fun getAllContentText(): String {
      return listOf(
        statusCard.getContentText(),
        reasonCard.getContentText()
      ).joinToString(System.lineSeparator())
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      val cardWidth = (
        measuredWidth -
          paddingStart -
          paddingEnd -
          cardGap
        ).coerceAtLeast(0) / 2
      statusCard.measure(
        cardWidth.toExactlyMeasureSpec(),
        statusCard.defaultHeightMeasureSpec(this)
      )
      reasonCard.measure(
        cardWidth.toExactlyMeasureSpec(),
        reasonCard.defaultHeightMeasureSpec(this)
      )

      val cardHeight = maxOf(statusCard.measuredHeight, reasonCard.measuredHeight)
      statusCard.measure(
        cardWidth.toExactlyMeasureSpec(),
        cardHeight.toExactlyMeasureSpec()
      )
      reasonCard.measure(
        cardWidth.toExactlyMeasureSpec(),
        cardHeight.toExactlyMeasureSpec()
      )
      setMeasuredDimension(
        measuredWidth,
        paddingTop +
          cardHeight +
          paddingBottom
      )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      statusCard.layout(paddingStart, paddingTop)
      reasonCard.layout(statusCard.right + cardGap, paddingTop)
    }
  }

  private class DexoptValueCardView(
    context: Context,
    label: String
  ) : MaterialCardView(context) {

    private val contentView = ContentView(context, label).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
    }

    init {
      setSmoothRoundCorner(10.dp)
      strokeWidth = 1.dp
      strokeColor = context.getColorByAttr(com.google.android.material.R.attr.colorOutlineVariant)
      setCardBackgroundColor(context.getColorStateListByAttr(com.google.android.material.R.attr.colorSurfaceContainer))
      addView(contentView)
    }

    fun setValue(value: String) {
      contentView.valueView.text = value
    }

    fun getContentText(): String {
      return "${contentView.labelView.text}${contentView.valueView.text}"
    }

    private class ContentView(context: Context, label: String) : AViewGroup(context) {

      val labelView = AppCompatTextView(
        ContextThemeWrapper(context, R.style.TextView_SansSerifMedium)
      ).apply {
        layoutParams = LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        text = label
      }

      val valueView = AppCompatTextView(
        ContextThemeWrapper(context, R.style.TextView_SansSerifCondensedMedium)
      ).apply {
        layoutParams = LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
      }

      init {
        setPadding(12.dp, 10.dp, 12.dp, 10.dp)
        addView(labelView)
        addView(valueView)
      }

      override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        labelView.autoMeasure()
        valueView.autoMeasure()

        val maxTextWidth = (measuredWidth - paddingStart - paddingEnd).coerceAtLeast(0)
        if (labelView.measuredWidth > maxTextWidth) {
          labelView.measure(
            maxTextWidth.toExactlyMeasureSpec(),
            labelView.defaultHeightMeasureSpec(this)
          )
        }
        if (valueView.measuredWidth > maxTextWidth) {
          valueView.measure(
            maxTextWidth.toExactlyMeasureSpec(),
            valueView.defaultHeightMeasureSpec(this)
          )
        }

        setMeasuredDimension(
          measuredWidth,
          paddingTop +
            labelView.measuredHeight +
            valueView.measuredHeight +
            paddingBottom
        )
      }

      override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        labelView.layout(paddingStart, paddingTop)
        valueView.layout(paddingStart, labelView.bottom)
      }
    }
  }
}
