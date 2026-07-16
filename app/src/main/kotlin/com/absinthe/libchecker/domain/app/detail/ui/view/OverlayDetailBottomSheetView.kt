package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import androidx.core.view.marginTop
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.domain.app.detail.model.OverlayDetailAction
import com.absinthe.libchecker.domain.app.detail.model.OverlayDetailBottomSheetDisplay
import com.absinthe.libchecker.domain.app.detail.model.OverlayDetailExtraInfoDisplay
import com.absinthe.libchecker.domain.app.detail.model.OverlayTargetPackageDisplay
import com.absinthe.libchecker.domain.app.detail.ui.binder.RelatedAppItemBinder
import com.absinthe.libchecker.domain.app.list.ui.view.AppItemView
import com.absinthe.libchecker.utils.extensions.copyToClipboard
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getColorStateListByAttr
import com.absinthe.libchecker.utils.extensions.setLongClickCopiedToClipboard
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import com.google.android.material.button.MaterialButton
import me.zhanghai.android.appiconloader.AppIconLoader

class OverlayDetailBottomSheetView(context: Context) :
  AViewGroup(context),
  IHeaderView {

  private val relatedAppItemBinder = RelatedAppItemBinder()

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    title.text = Constants.OVERLAY_STRING
  }

  private val detailsTitleView = DetailsTitleView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 24.dp
    }
  }

  private val targetTitleView = AppCompatTextView(
    ContextThemeWrapper(context, R.style.TextView_SansSerifMedium)
  ).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 8.dp
    }
    //noinspection SetTextI18n
    text = "Target Package"
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
  }

  private val targetPackageView = AppItemView(context).apply {
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

  private val moreDetailButton = MaterialButton(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 8.dp
    }
    text = context.getString(R.string.lib_detail_elf_info)
    rippleColor = context.getColorStateListByAttr(com.google.android.material.R.attr.colorOnSecondaryContainer)
  }

  init {
    setPadding(24.dp, 16.dp, 24.dp, 16.dp)
    addView(header)
    addView(detailsTitleView)
    addView(targetTitleView)
    addView(targetPackageView)
    addView(moreDetailButton)
  }

  fun bind(
    display: OverlayDetailBottomSheetDisplay,
    onAction: (OverlayDetailAction) -> Unit
  ) {
    bindDetail(display)
    bindTarget(display.target, onAction)
    moreDetailButton.setOnClickListener {
      onAction(OverlayDetailAction.OpenApp(display.item, forceDetail = true))
    }
    requestLayout()
  }

  private fun bindDetail(display: OverlayDetailBottomSheetDisplay) {
    detailsTitleView.apply {
      iconView.apply {
        val appIconLoader = AppIconLoader(
          resources.getDimensionPixelSize(R.dimen.lib_detail_icon_size),
          false,
          context
        )
        display.applicationInfo?.let {
          load(appIconLoader.loadIcon(it))
        }
        setOnLongClickListener {
          copyToClipboard()
          true
        }
      }
      appNameView.apply {
        text = display.appName
        setLongClickCopiedToClipboard(text)
      }
      iconView.contentDescription = appNameView.text
      packageNameView.apply {
        text = display.packageName
        setLongClickCopiedToClipboard(text)
      }
      versionInfoView.apply {
        text = display.versionInfo
        setLongClickCopiedToClipboard(text)
      }
      extraInfoView.text = buildOverlayExtraInfo(display.extraInfo)
    }
  }

  private fun bindTarget(
    target: OverlayTargetPackageDisplay,
    onAction: (OverlayDetailAction) -> Unit
  ) {
    when (target) {
      is OverlayTargetPackageDisplay.RelatedApp -> {
        relatedAppItemBinder.bind(
          appItemView = targetPackageView,
          title = targetTitleView.text,
          data = target.data,
          showHarmonyBadge = target.showHarmonyBadge
        ) {
          onAction(OverlayDetailAction.OpenApp(target.data.item, forceDetail = false))
        }
      }

      is OverlayTargetPackageDisplay.PackageName -> {
        targetPackageView.setOnClickListener(null)
        targetPackageView.addFloatView(target.value)
      }

      OverlayTargetPackageDisplay.Empty -> {
        targetPackageView.setOnClickListener(null)
      }
    }
  }

  private fun buildOverlayExtraInfo(extraInfo: OverlayDetailExtraInfoDisplay) = buildSpannedString {
    append(extraInfo.type).append(", ")
    scale(0.8f) {
      append("Target: ")
    }
    append(extraInfo.targetSdkInfo)
    scale(0.8f) {
      append(" Min: ")
    }
    append(extraInfo.minSdkInfo)
    scale(0.8f) {
      append(" Compile: ")
    }
    append(extraInfo.compileSdkInfo)
    scale(0.8f) {
      append(" Size: ")
    }
    append(extraInfo.sizeInfo)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    header.autoMeasure()
    detailsTitleView.measure(
      (measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(),
      detailsTitleView.defaultHeightMeasureSpec(this)
    )
    targetTitleView.autoMeasure()
    targetPackageView.measure(
      (measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(),
      targetPackageView.defaultHeightMeasureSpec(this)
    )
    moreDetailButton.measure(
      (measuredWidth - paddingStart - paddingEnd).toExactlyMeasureSpec(),
      moreDetailButton.defaultHeightMeasureSpec(this)
    )
    setMeasuredDimension(
      measuredWidth,
      paddingTop +
        header.measuredHeight +
        detailsTitleView.marginTop +
        detailsTitleView.measuredHeight +
        targetTitleView.marginTop +
        targetTitleView.measuredHeight +
        targetPackageView.marginTop +
        targetPackageView.measuredHeight +
        moreDetailButton.marginTop +
        moreDetailButton.measuredHeight +
        paddingBottom
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    header.layout(0, paddingTop)
    detailsTitleView.layout(paddingStart, header.bottom + detailsTitleView.marginTop)
    targetTitleView.layout(paddingStart, detailsTitleView.bottom + targetTitleView.marginTop)
    targetPackageView.layout(paddingStart, targetTitleView.bottom + targetPackageView.marginTop)
    moreDetailButton.layout(paddingStart, targetPackageView.bottom + moreDetailButton.marginTop)
  }

  override fun getHeaderView(): BottomSheetHeaderView = header
}
