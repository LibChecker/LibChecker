package com.absinthe.libchecker.features.applist.detail.ui.view

import android.content.Context
import android.graphics.Color
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

class XposedInfoBottomSheetView(context: Context) : AViewGroup(context), IHeaderView {

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.xposed_module)
  }

  val setting = AppInfoItemView(context).apply {
    layoutParams = LayoutParams(54.dp, ViewGroup.LayoutParams.WRAP_CONTENT)
    setIcon(R.drawable.ic_settings)
    setIconBackgroundTintColor(R.color.material_blue_grey_300)
    setIconTintColor(Color.WHITE)
    setText("")
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
      it.topMargin = (-24).dp
    }
    gravity = Gravity.CENTER
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
  }

  val xposedDetailContentView = XposedDetailContentView(context).apply {
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
    addView(setting)
    addView(title)
    addView(xposedDetailContentView)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    header.measure(
      (measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(),
      header.defaultHeightMeasureSpec(this)
    )
    setting.autoMeasure()
    title.measure(
      (measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(),
      title.defaultHeightMeasureSpec(this)
    )
    xposedDetailContentView.measure(
      (measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(),
      xposedDetailContentView.defaultHeightMeasureSpec(this)
    )
    setMeasuredDimension(
      measuredWidth,
      paddingTop + paddingBottom + header.measuredHeight + title.measuredHeight + setting.measuredHeight + title.marginTop + setting.marginTop + xposedDetailContentView.measuredHeight + 16.dp
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    header.layout(paddingStart, paddingTop)
    setting.layout(setting.toHorizontalCenter(this), header.bottom + setting.marginTop)
    title.layout(title.toHorizontalCenter(this), setting.bottom + title.marginTop)
    xposedDetailContentView.layout(paddingStart, title.bottom.coerceAtLeast(setting.bottom) + 16.dp)
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }

  class XposedDetailItemView(context: Context) : AViewGroup(context) {

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

  class XposedDetailContentView(context: Context) : AViewGroup(context) {

    val minVersion = XposedDetailItemView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      icon.setImageResource(R.drawable.ic_app_prop)
      tip.text = context.getString(R.string.lib_detail_xposed_min_version)
      text.setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceSubtitle2))
    }

    val scope = XposedDetailItemView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      icon.setImageResource(R.drawable.ic_app_prop)
      tip.text = context.getString(R.string.lib_detail_xposed_default_scope)
      text.text = context.getString(R.string.empty_list)
      text.setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceSubtitle2))
    }

    val initClass = XposedDetailItemView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      icon.setImageResource(R.drawable.ic_app_prop)
      tip.text = context.getString(R.string.lib_detail_xposed_init_class)
      text.setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceSubtitle2))
    }

    val description = XposedDetailItemView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      icon.setImageResource(R.drawable.ic_content)
      tip.text = context.getString(R.string.lib_detail_description_tip)
      text.setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceBody2))
    }

    init {
      addView(minVersion)
      addView(scope)
      addView(initClass)
      addView(description)
    }

    private val marginVertical = 8.dp

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      minVersion.measure(
        (measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(),
        minVersion.defaultHeightMeasureSpec(this)
      )
      scope.measure(
        (measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(),
        scope.defaultHeightMeasureSpec(this)
      )
      initClass.measure(
        (measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(),
        initClass.defaultHeightMeasureSpec(this)
      )
      description.measure(
        (measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(),
        description.defaultHeightMeasureSpec(this)
      )
      setMeasuredDimension(
        measuredWidth,
        minVersion.measuredHeight + scope.measuredHeight +
          initClass.measuredHeight +
          description.measuredHeight + marginVertical * 5
      )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      minVersion.layout(0, 0)
      scope.layout(0, minVersion.bottom + marginVertical)
      initClass.layout(0, scope.bottom + marginVertical)
      description.layout(0, initClass.bottom + marginVertical)
    }
  }
}
