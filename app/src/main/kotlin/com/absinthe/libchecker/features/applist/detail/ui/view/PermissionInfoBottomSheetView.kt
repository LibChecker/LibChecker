package com.absinthe.libchecker.features.applist.detail.ui.view

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.manager.SystemBarManager
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import com.absinthe.libraries.utils.view.HeightAnimatableViewFlipper

class PermissionInfoBottomSheetView(context: Context) :
  AViewGroup(context),
  IHeaderView {

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.lib_permission_dialog_title)
  }

  val icon = AppCompatImageView(context).apply {
    val iconSize = 48.dp
    layoutParams = LayoutParams(iconSize, iconSize).also {
      it.topMargin = 4.dp
    }
    setBackgroundResource(R.drawable.bg_circle_outline)
  }

  val title = AppCompatTextView(
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
    gravity = Gravity.CENTER
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
  }

  private val viewFlipper = HeightAnimatableViewFlipper(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    setInAnimation(context, R.anim.anim_fade_in)
    setOutAnimation(context, R.anim.anim_fade_out)
  }

  private val notFoundView = NotFoundView(context).apply {
    layoutParams = FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT,
      FrameLayout.LayoutParams.MATCH_PARENT
    ).also {
      it.gravity = Gravity.CENTER
    }
  }

  val permissionContentView = PermissionContentView(context).apply {
    layoutParams = FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT,
      FrameLayout.LayoutParams.WRAP_CONTENT
    )
  }

  init {
    val padding = 16.dp
    setPadding(
      padding,
      padding,
      padding,
      (padding - SystemBarManager.navigationBarSize).coerceAtLeast(0)
    )
    addView(header)
    addView(icon)
    addView(title)
    addView(viewFlipper)
    viewFlipper.addView(permissionContentView)
    viewFlipper.addView(notFoundView)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    header.measure(
      (measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(),
      header.defaultHeightMeasureSpec(this)
    )
    icon.autoMeasure()
    title.measure(
      (measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(),
      title.defaultHeightMeasureSpec(this)
    )
    viewFlipper.measure(
      (measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(),
      viewFlipper.defaultHeightMeasureSpec(this)
    )
    setMeasuredDimension(
      measuredWidth,
      paddingTop + paddingBottom + header.measuredHeight + title.measuredHeight + icon.measuredHeight + title.marginTop + icon.marginTop + viewFlipper.measuredHeight + 16.dp
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    header.layout(paddingStart, paddingTop)
    icon.layout(icon.toHorizontalCenter(this), header.bottom + icon.marginTop)
    title.layout(title.toHorizontalCenter(this), icon.bottom + title.marginTop)
    viewFlipper.layout(paddingStart, title.bottom.coerceAtLeast(icon.bottom) + 16.dp)
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }

  class PermissionItemView(context: Context) : AViewGroup(context) {

    val icon = AppCompatImageView(context).apply {
      layoutParams = LayoutParams(24.dp, 24.dp)
    }

    val tip = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).also {
        it.marginStart = 8.dp
      }
      alpha = 0.65f
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
    }

    val text = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).also {
        it.marginStart = 8.dp
        it.topMargin = 0
      }
    }

    init {
      setPadding(8.dp, 8.dp, 8.dp, 8.dp)
      setBackgroundResource(R.drawable.bg_lib_detail_item)
      addView(icon)
      addView(tip)
      addView(text)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      icon.autoMeasure()
      tip.measure(
        (measuredWidth - paddingStart - paddingEnd - icon.measuredWidth - tip.marginStart).toExactlyMeasureSpec(),
        tip.defaultHeightMeasureSpec(this)
      )
      text.measure(
        (measuredWidth - paddingStart - paddingEnd - icon.measuredWidth - text.marginStart).toExactlyMeasureSpec(),
        text.defaultHeightMeasureSpec(this)
      )
      setMeasuredDimension(
        measuredWidth,
        (tip.measuredHeight + text.marginTop + text.measuredHeight).coerceAtLeast(icon.measuredHeight) + paddingTop + paddingBottom
      )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      icon.layout(paddingStart, icon.toVerticalCenter(this))
      tip.layout(icon.right + tip.marginStart, paddingTop)
      text.layout(tip.left, tip.bottom + text.marginTop)
    }
  }

  class NotFoundView(context: Context) : AViewGroup(context) {

    private val icon = AppCompatImageView(context).apply {
      layoutParams = LayoutParams(64.dp, 64.dp)
      setImageResource(R.drawable.ic_failed)
    }

    private val notFoundText = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      text = context.getString(R.string.not_found)
      setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceBody2))
    }

    init {
      addView(icon)
      addView(notFoundText)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      icon.autoMeasure()
      notFoundText.autoMeasure()
      setMeasuredDimension(
        measuredWidth,
        icon.measuredHeight + notFoundText.measuredHeight
      )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      icon.layout(icon.toHorizontalCenter(this), 0)
      notFoundText.layout(notFoundText.toHorizontalCenter(this), icon.bottom)
    }
  }

  class PermissionContentView(context: Context) : AViewGroup(context) {

    val label = PermissionItemView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      icon.setImageResource(R.drawable.ic_label)
      tip.text = context.getString(R.string.lib_detail_label_tip)
      text.setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceSubtitle2))
    }

    val description = PermissionItemView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      icon.setImageResource(R.drawable.ic_content)
      tip.text = context.getString(R.string.lib_detail_description_tip)
      text.setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceBody2))
    }

    init {
      addView(label)
      addView(description)
    }

    private val marginVertical = 8.dp

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      label.measure(
        (measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(),
        label.defaultHeightMeasureSpec(this)
      )
      description.measure(
        (measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(),
        description.defaultHeightMeasureSpec(this)
      )
      setMeasuredDimension(
        measuredWidth,
        label.measuredHeight + description.measuredHeight + marginVertical
      )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      label.layout(0, 0)
      description.layout(0, label.bottom + marginVertical)
    }
  }

  fun showNotFound() {
    if (viewFlipper.displayedChildView != notFoundView) {
      viewFlipper.show(notFoundView)
    }
  }
}
