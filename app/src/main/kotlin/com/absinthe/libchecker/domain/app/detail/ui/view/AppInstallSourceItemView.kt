package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.marginTop
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.AppInstallSourceAction
import com.absinthe.libchecker.domain.app.detail.model.AppInstallSourceItemContent
import com.absinthe.libchecker.domain.app.detail.model.AppInstallSourceItemDisplay
import com.absinthe.libchecker.domain.app.detail.ui.binder.RelatedAppItemBinder
import com.absinthe.libchecker.domain.app.list.ui.view.AppItemView
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getColorStateListByAttr
import com.absinthe.libchecker.view.AViewGroup

class AppInstallSourceItemView(
  context: Context,
  initialTitle: CharSequence = ""
) : AViewGroup(context) {

  private val relatedAppItemBinder = RelatedAppItemBinder()

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
    text = initialTitle
  }

  private val packageView = AppItemView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 8.dp
    }
    strokeWidth = 1.dp
    strokeColor = context.getColorByAttr(com.google.android.material.R.attr.colorOutlineVariant)
    setCardBackgroundColor(context.getColorStateListByAttr(com.google.android.material.R.attr.colorSurfaceContainerHigh))
  }

  init {
    addView(titleView)
    addView(packageView)
  }

  fun bind(
    display: AppInstallSourceItemDisplay,
    onAction: (AppInstallSourceAction) -> Unit
  ) {
    titleView.text = display.title
    when (val content = display.content) {
      is AppInstallSourceItemContent.RelatedApp -> {
        relatedAppItemBinder.bind(
          appItemView = packageView,
          title = display.title,
          data = content.data
        ) {
          display.action?.let(onAction)
        }
      }

      is AppInstallSourceItemContent.Message -> bindMessage(content)
    }
    packageView.setItemContentDescription(display.contentDescription)
    val action = display.action
    if (action == null) {
      packageView.setOnClickListener(null)
    } else {
      packageView.setOnClickListener { onAction(action) }
    }
  }

  private fun bindMessage(content: AppInstallSourceItemContent.Message) {
    packageView.container.apply {
      icon.load(content.iconRes)
      setAppName(content.appName)
      setPackageName(content.packageName)
      setVersionInfo(content.versionInfo)
      setAbiInfo(content.abiInfo)
      abiInfo.isVisible = content.showAbiInfo
      setBadge(null)
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val parent = parent as ViewGroup
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    titleView.autoMeasure()
    packageView.let {
      it.measure(
        (measuredWidth - parent.paddingStart - parent.paddingEnd).toExactlyMeasureSpec(),
        if (it.isGone) 0 else it.defaultHeightMeasureSpec(parent)
      )
    }
    setMeasuredDimension(
      measuredWidth,
      titleView.marginTop +
        titleView.measuredHeight +
        packageView.marginTop +
        packageView.measuredHeight
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    titleView.layout(paddingStart, titleView.marginTop)
    packageView.layout(paddingStart, titleView.bottom + packageView.marginTop)
  }
}
