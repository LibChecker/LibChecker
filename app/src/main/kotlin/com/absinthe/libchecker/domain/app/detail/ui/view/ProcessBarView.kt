package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isGone
import androidx.core.view.marginStart
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.ProcessBarAction
import com.absinthe.libchecker.domain.app.detail.model.ProcessBarItemRenderState
import com.absinthe.libchecker.domain.app.detail.model.ProcessBarRenderState
import com.absinthe.libchecker.domain.app.detail.ui.adapter.ProcessBarAdapter
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.utils.extensions.tintTextToPrimary
import com.absinthe.libchecker.utils.extensions.toColorStateListByColor
import com.absinthe.libchecker.view.AViewGroup

class ProcessBarView(context: Context) : RecyclerView(context) {

  private val processBarAdapter = ProcessBarAdapter()
  private var onAction: (ProcessBarAction) -> Unit = {}

  init {
    isHorizontalScrollBarEnabled = false
    layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
    adapter = processBarAdapter
    setPadding(8.dp, 0, 8.dp, 4.dp)
    clipToPadding = false
    clipChildren = false
    processBarAdapter.setOnItemClickListener { _, _, position ->
      val item = processBarAdapter.getItem(position)
      onAction(
        ProcessBarAction.ProcessSelectionChanged(
          process = item.process.takeUnless { item.selected }
        )
      )
    }
  }

  fun bind(
    state: ProcessBarRenderState,
    onAction: (ProcessBarAction) -> Unit
  ) {
    this.onAction = onAction
    isGone = !state.visible
    processBarAdapter.setList(state.items)
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
      importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
      addView(this)
    }

    private val text = AppCompatTextView(
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

    private fun setIndicatorColor(color: Int) {
      colorIndicator.imageTintList = color.toColorStateListByColor()
    }

    fun bind(item: ProcessBarItemRenderState) {
      setIndicatorColor(item.color)
      text.text = item.process
      contentDescription = item.process
      isSelected = item.selected
      setTarget(item.selected)
    }

    private fun setTarget(target: Boolean) {
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
