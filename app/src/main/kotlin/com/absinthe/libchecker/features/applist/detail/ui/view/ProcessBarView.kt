package com.absinthe.libchecker.features.applist.detail.ui.view

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginStart
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.features.applist.detail.ui.adapter.ProcessBarAdapter
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.utils.extensions.tintTextToPrimary
import com.absinthe.libchecker.utils.extensions.toColorStateListByColor
import com.absinthe.libchecker.view.AViewGroup

class ProcessBarView(context: Context) : RecyclerView(context) {

  private val processBarAdapter = ProcessBarAdapter()

  init {
    isHorizontalScrollBarEnabled = false
    layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
    adapter = processBarAdapter
    setPadding(8.dp, 0, 8.dp, 4.dp)
    clipToPadding = false
    clipChildren = false
  }

  fun setData(data: List<ProcessBarAdapter.ProcessBarItem>) {
    processBarAdapter.setList(data)
  }

  fun setOnItemClickListener(action: (isSelected: Boolean, processName: String) -> Unit) {
    processBarAdapter.setOnItemClickListener { _, view, position ->
      view.isSelected = !view.isSelected
      (view as? ProcessBarItemView)?.setTarget(view.isSelected)
      action(
        view.isSelected,
        processBarAdapter.data[position].process
      )

      (0 until processBarAdapter.itemCount).forEach {
        if (it != position) {
          findViewHolderForLayoutPosition(it)?.itemView?.let { itemView ->
            isSelected = false
            (itemView as? ProcessBarItemView)?.setTarget(false)
          }
        }
      }
    }
  }

  class ProcessBarItemView(context: Context) : AViewGroup(context) {

    init {
      setPadding(4.dp, 4.dp, 4.dp, 4.dp)
      isClickable = true
      isFocusable = true
      setBackgroundResource(context.getResourceIdByAttr(android.R.attr.selectableItemBackgroundBorderless))
    }

    private val colorIndicator = AppCompatImageView(context).apply {
      layoutParams = LayoutParams(18.dp, 18.dp)
      setImageResource(R.drawable.bg_circle_secondary_container)
      setPadding(2.dp, 2.dp, 2.dp, 2.dp)
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
        it.marginStart = 2.dp
      }
      setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceLabelSmall))
      addView(this)
    }

    private val animator by lazy {
      ScaleAnimation(
        1f,
        1.1f,
        1f,
        1.1f,
        ScaleAnimation.RELATIVE_TO_SELF,
        0.5f,
        ScaleAnimation.RELATIVE_TO_SELF,
        0.5f
      ).apply {
        duration = 1000
        repeatCount = Animation.INFINITE
        repeatMode = ScaleAnimation.REVERSE
      }
    }

    fun setIndicatorColor(color: Int) {
      colorIndicator.imageTintList = color.toColorStateListByColor()
    }

    fun setTarget(target: Boolean) {
      if (target) {
        colorIndicator.startAnimation(animator)
        text.tintTextToPrimary()
      } else {
        colorIndicator.clearAnimation()
        text.text = text.text.toString()
      }
    }

    override fun onDetachedFromWindow() {
      super.onDetachedFromWindow()
      colorIndicator.clearAnimation()
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
