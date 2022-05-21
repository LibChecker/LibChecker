package com.absinthe.libchecker.view.detail

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginStart
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.recyclerview.adapter.detail.ProcessBarAdapter
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.utils.extensions.toColorStateListByColor
import com.absinthe.libchecker.view.AViewGroup
import timber.log.Timber

class ProcessBarView(context: Context) : RecyclerView(context) {

  private val processBarAdapter = ProcessBarAdapter()

  init {
    isHorizontalScrollBarEnabled = false
    layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
    adapter = processBarAdapter
    setPadding(8.dp, 0, 8.dp, 12.dp)
  }

  fun setData(data: List<ProcessBarAdapter.ProcessBarItem>) {
    processBarAdapter.setList(data)
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    Timber.d("onSizeChanged: $w, $h, $oldw, $oldh")
  }

  class ProcessBarItemView(context: Context) : AViewGroup(context) {

    init {
      setPadding(4.dp, 0, 4.dp, 0)
    }

    private val colorIndicator = View(context).apply {
      layoutParams = LayoutParams(16.dp, 16.dp)
      setBackgroundResource(R.drawable.bg_gray_circle)
      addView(this)
    }

    val text = AppCompatTextView(
      ContextThemeWrapper(
        context,
        R.style.TextView_SansSerifCondensedMedium
      )
    ).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).also {
        it.topMargin = 4.dp
      }
      setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceLabelSmall))
      addView(this)
    }

    fun setIndicatorColor(color: Int) {
      colorIndicator.backgroundTintList = color.toColorStateListByColor()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      colorIndicator.autoMeasure()
      text.autoMeasure()
      setMeasuredDimension(
        paddingStart + colorIndicator.measuredWidth + text.marginStart + text.measuredWidth + paddingEnd,
        paddingTop + paddingBottom + colorIndicator.measuredHeight.coerceAtLeast(text.measuredHeight)
      )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      colorIndicator.layout(paddingStart, colorIndicator.toVerticalCenter(this))
      text.layout(colorIndicator.right + text.marginStart, text.toVerticalCenter(this))
    }
  }
}
