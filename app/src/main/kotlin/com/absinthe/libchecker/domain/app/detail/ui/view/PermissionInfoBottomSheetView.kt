package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.buildSpannedString
import androidx.core.view.marginTop
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.DetailInfoItemDisplay
import com.absinthe.libchecker.domain.app.detail.model.DetailInfoTextStyle
import com.absinthe.libchecker.domain.app.detail.model.PermissionDetailBottomSheetState
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.manager.SystemBarManager
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class PermissionInfoBottomSheetView(context: Context) :
  AViewGroup(context),
  IHeaderView {

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.lib_permission_dialog_title)
  }

  private val icon = AppCompatImageView(context).apply {
    val iconSize = 48.dp
    layoutParams = LayoutParams(iconSize, iconSize).also {
      it.topMargin = 4.dp
    }
    setBackgroundResource(R.drawable.bg_circle_outline)
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
  }

  private val title = AppCompatTextView(
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

  private val permissionContentView = PermissionContentView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
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
    addView(permissionContentView)
  }

  fun bind(state: PermissionDetailBottomSheetState) {
    val detail = (state as? PermissionDetailBottomSheetState.Content)?.detail
    icon.load(detail?.icon ?: com.absinthe.lc.rulesbundle.R.drawable.ic_lib_android)
    title.text = buildSpannedString {
      append(state.permissionName)
      detail?.providerAppName?.let {
        appendLine()
        append(context.getString(R.string.lib_permission_provided_by_format, it))
      }
    }
    permissionContentView.bind(
      label = detail?.label ?: context.getText(R.string.not_found),
      description = detail?.description ?: context.getText(R.string.not_found)
    )
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
    permissionContentView.measure(
      (measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(),
      permissionContentView.defaultHeightMeasureSpec(this)
    )
    setMeasuredDimension(
      measuredWidth,
      paddingTop + paddingBottom + header.measuredHeight + title.measuredHeight + icon.measuredHeight + title.marginTop + icon.marginTop + permissionContentView.measuredHeight + 16.dp
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    header.layout(paddingStart, paddingTop)
    icon.layout(icon.toHorizontalCenter(this), header.bottom + icon.marginTop)
    title.layout(title.toHorizontalCenter(this), icon.bottom + title.marginTop)
    permissionContentView.layout(paddingStart, title.bottom.coerceAtLeast(icon.bottom) + 16.dp)
  }

  override fun getHeaderView(): BottomSheetHeaderView = header

  private class PermissionContentView(context: Context) : AViewGroup(context) {

    private val label = DetailInfoItemView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
    }

    private val description = DetailInfoItemView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
    }

    private val marginVertical = 8.dp

    init {
      addView(label)
      addView(description)
    }

    fun bind(label: CharSequence, description: CharSequence) {
      this.label.bind(
        DetailInfoItemDisplay(
          iconRes = R.drawable.ic_label,
          tipRes = R.string.lib_detail_label_tip,
          textStyle = DetailInfoTextStyle.TITLE,
          text = label
        )
      )
      this.description.bind(
        DetailInfoItemDisplay(
          iconRes = R.drawable.ic_content,
          tipRes = R.string.lib_detail_description_tip,
          textStyle = DetailInfoTextStyle.BODY,
          text = description
        )
      )
    }

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
}
