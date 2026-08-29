package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.isVisible
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.XposedInfoAction
import com.absinthe.libchecker.domain.app.detail.model.XposedInfoBottomSheetDisplay
import com.absinthe.libchecker.domain.app.detail.model.XposedInfoItemDisplay
import com.absinthe.libchecker.domain.app.detail.model.XposedInfoTextStyle
import com.absinthe.libchecker.domain.app.detail.model.XposedScopeAppDisplay
import com.absinthe.libchecker.domain.app.detail.model.buildDetailItemDescription
import com.absinthe.libchecker.ui.adapter.BindOnlyAdapter
import com.absinthe.libchecker.ui.adapter.addSpacingDecoration
import com.absinthe.libchecker.ui.app.BottomSheetRecyclerView
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.app.AppIconPlaceholder
import com.absinthe.libchecker.view.app.BottomSheetScaffoldView
import com.absinthe.libraries.utils.manager.SystemBarManager

class XposedInfoBottomSheetView(context: Context) : BottomSheetScaffoldView(context) {

  private val contentAdapter = BindOnlyAdapter(::XposedDetailItemView, XposedDetailItemView::bind)

  private val setting = AppInfoItemView(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.WRAP_CONTENT,
      LayoutParams.WRAP_CONTENT
    )
    setIcon(R.drawable.ic_settings)
    setIconTintColor(
      context.getColorByAttr(com.google.android.material.R.attr.colorOnSecondaryContainer)
    )
  }

  private val xposedDetailContentView = BottomSheetRecyclerView(context).apply {
    layoutParams = FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.MATCH_PARENT,
      FrameLayout.LayoutParams.WRAP_CONTENT
    )
    adapter = contentAdapter
    overScrollMode = OVER_SCROLL_NEVER
    layoutManager = LinearLayoutManager(context)
    isVerticalScrollBarEnabled = false
    clipToPadding = false
    clipChildren = false
    addSpacingDecoration(4.dp)
  }

  init {
    gravity = Gravity.CENTER_HORIZONTAL
    val padding = 16.dp
    setPadding(
      padding,
      padding,
      padding,
      (padding - SystemBarManager.navigationBarSize).coerceAtLeast(0)
    )
    header.title.text = context.getString(R.string.xposed_module)
    addView(setting)
    addView(xposedDetailContentView)
  }

  fun bind(
    display: XposedInfoBottomSheetDisplay,
    onAction: (XposedInfoAction) -> Unit
  ) {
    setting.setText(display.appName)
    setting.setOnClickListener {
      onAction(display.settingsAction)
    }
    contentAdapter.setList(display.items)
  }

  class XposedDetailItemView(context: Context) : AViewGroup(context) {

    private val icon = AppCompatImageView(context).apply {
      layoutParams = LayoutParams(24.dp, 24.dp)
      importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private val tip = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).also {
        it.marginStart = 8.dp
      }
      alpha = 0.65f
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
    }

    private val text = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).also {
        it.marginStart = 8.dp
        it.topMargin = 0
      }
    }

    private val scopeContainer = LinearLayout(context).apply {
      orientation = HORIZONTAL
      gravity = Gravity.CENTER_VERTICAL
      importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private val scopeScroll = HorizontalScrollView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        SCOPE_ICON_TOUCH_SIZE
      ).also {
        it.marginStart = 8.dp
        it.topMargin = 4.dp
      }
      overScrollMode = OVER_SCROLL_NEVER
      isHorizontalScrollBarEnabled = false
      isHorizontalFadingEdgeEnabled = true
      setFadingEdgeLength(16.dp)
      clipToPadding = false
      isVisible = false
      addView(
        scopeContainer,
        ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT,
          ViewGroup.LayoutParams.MATCH_PARENT
        )
      )
    }

    init {
      setPadding(8.dp, 8.dp, 8.dp, 8.dp)
      setBackgroundResource(R.drawable.bg_lib_detail_item)
      addView(icon)
      addView(tip)
      addView(text)
      addView(scopeScroll)
    }

    fun bind(item: XposedInfoItemDisplay) {
      icon.setImageResource(item.iconRes)
      when (item) {
        is XposedInfoItemDisplay.Text -> bindText(item)
        is XposedInfoItemDisplay.ScopeApps -> bindScopeApps(item)
      }
    }

    private fun bindText(item: XposedInfoItemDisplay.Text) {
      tip.text = item.tip
      text.isVisible = true
      text.text = item.text
      text.setTextAppearance(
        context.getResourceIdByAttr(
          when (item.textStyle) {
            XposedInfoTextStyle.Title -> com.google.android.material.R.attr.textAppearanceTitleSmall
            XposedInfoTextStyle.Body -> com.google.android.material.R.attr.textAppearanceBodyMedium
          }
        )
      )
      scopeScroll.isVisible = false
      scopeContainer.removeAllViews()
      contentDescription = buildDetailItemDescription(tip.text, text.text)
    }

    private fun bindScopeApps(item: XposedInfoItemDisplay.ScopeApps) {
      tip.text = "${item.tip} · ${item.apps.size}"
      text.isVisible = false
      scopeScroll.isVisible = true
      scopeScroll.scrollTo(0, 0)
      contentDescription = null
      scopeContainer.removeAllViews()
      item.apps.forEachIndexed { index, app ->
        scopeContainer.addView(createScopeIcon(app, index == item.apps.lastIndex))
      }
    }

    private fun createScopeIcon(
      app: XposedScopeAppDisplay,
      isLast: Boolean
    ): AppCompatImageView {
      return AppCompatImageView(context).apply {
        layoutParams = LinearLayout.LayoutParams(
          SCOPE_ICON_TOUCH_SIZE,
          SCOPE_ICON_TOUCH_SIZE
        ).also {
          it.marginEnd = if (isLast) 0 else SCOPE_ICON_GAP
        }
        setPadding(SCOPE_ICON_PADDING, SCOPE_ICON_PADDING, SCOPE_ICON_PADDING, SCOPE_ICON_PADDING)
        scaleType = ImageView.ScaleType.FIT_CENTER
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        val tooltip = if (app.label == app.packageName) {
          app.packageName
        } else {
          "${app.label}\n${app.packageName}"
        }
        contentDescription = tooltip
        TooltipCompat.setTooltipText(this, tooltip)
        val packageInfo = app.packageInfo
        if (packageInfo == null) {
          setImageResource(AppIconPlaceholder.resourceId)
        } else {
          load(packageInfo) {
            placeholder(AppIconPlaceholder.resourceId)
            error(AppIconPlaceholder.resourceId)
            crossfade(false)
          }
        }
      }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      autoMeasureChildren()
      val textWidth = measuredWidth - paddingStart - paddingEnd - icon.measuredWidth - tip.marginStart
      if (tip.measuredWidth > textWidth) {
        tip.measure(textWidth.toExactlyMeasureSpec(), tip.defaultHeightMeasureSpec(this))
      }
      if (text.isVisible && text.measuredWidth > textWidth) {
        text.measure(textWidth.toExactlyMeasureSpec(), text.defaultHeightMeasureSpec(this))
      }
      if (scopeScroll.isVisible) {
        scopeScroll.measure(
          textWidth.toExactlyMeasureSpec(),
          SCOPE_ICON_TOUCH_SIZE.toExactlyMeasureSpec()
        )
      }
      val detailHeight = if (scopeScroll.isVisible) {
        scopeScroll.marginTop + scopeScroll.measuredHeight
      } else {
        text.marginTop + text.measuredHeight
      }
      setMeasuredDimension(
        measuredWidth,
        (tip.measuredHeight + detailHeight).coerceAtLeast(icon.measuredHeight) +
          paddingTop +
          paddingBottom
      )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      icon.layout(paddingStart, icon.toVerticalCenter(this))
      tip.layout(paddingStart + icon.measuredWidth + tip.marginStart, paddingTop)
      if (scopeScroll.isVisible) {
        scopeScroll.layout(
          paddingStart + icon.measuredWidth + scopeScroll.marginStart,
          tip.bottom + scopeScroll.marginTop
        )
      } else {
        text.layout(paddingStart + icon.measuredWidth + text.marginStart, tip.bottom + text.marginTop)
      }
    }

    private companion object {
      val SCOPE_ICON_TOUCH_SIZE = 40.dp
      val SCOPE_ICON_PADDING = 4.dp
      val SCOPE_ICON_GAP = 8.dp
    }
  }
}
