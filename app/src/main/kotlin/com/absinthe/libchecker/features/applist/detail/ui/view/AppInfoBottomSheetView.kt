package com.absinthe.libchecker.features.applist.detail.ui.view

import android.content.Context
import android.graphics.Color
import android.widget.LinearLayout
import com.absinthe.libchecker.R
import com.absinthe.libchecker.ui.app.BottomSheetRecyclerView
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.manager.SystemBarManager
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import com.google.android.flexbox.FlexboxLayout

class AppInfoBottomSheetView(context: Context) :
  LinearLayout(context),
  IHeaderView {

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.further_operation)
  }

  private val functionsContainer = FlexboxLayout(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 24.dp
    }
  }

  val launch = AppInfoItemView(context).apply {
    layoutParams = FlexboxLayout.LayoutParams(
      FlexboxLayout.LayoutParams.WRAP_CONTENT,
      FlexboxLayout.LayoutParams.WRAP_CONTENT
    ).apply {
      flexBasisPercent = 0.25f
    }
    setIcon(R.drawable.ic_launch)
    setIconBackgroundTintColor(R.color.material_deep_purple_300)
    setIconTintColor(Color.WHITE)
    setText(R.string.app_info_launch)
  }

  val setting = AppInfoItemView(context).apply {
    layoutParams = FlexboxLayout.LayoutParams(
      FlexboxLayout.LayoutParams.WRAP_CONTENT,
      FlexboxLayout.LayoutParams.WRAP_CONTENT
    ).apply {
      flexBasisPercent = 0.25f
    }
    setIcon(R.drawable.ic_settings)
    setIconBackgroundTintColor(R.color.material_blue_grey_300)
    setIconTintColor(Color.WHITE)
    setText(R.string.app_info_settings)
  }

  val share = AppInfoItemView(context).apply {
    layoutParams = FlexboxLayout.LayoutParams(
      FlexboxLayout.LayoutParams.WRAP_CONTENT,
      FlexboxLayout.LayoutParams.WRAP_CONTENT
    ).apply {
      flexBasisPercent = 0.25f
    }
    setIcon(R.drawable.ic_share)
    setIconBackgroundTintColor(R.color.material_indigo_300)
    setIconTintColor(Color.WHITE)
    setText(R.string.app_info_share)
  }

  val list = BottomSheetRecyclerView(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    )
    overScrollMode = OVER_SCROLL_NEVER
  }

  init {
    orientation = VERTICAL
    setPadding(
      24.dp,
      16.dp,
      24.dp,
      (16.dp - SystemBarManager.navigationBarSize).coerceAtLeast(0)
    )
    addView(header)
    addView(functionsContainer)
    functionsContainer.addView(launch)
    functionsContainer.addView(setting)
    functionsContainer.addView(share)
    addView(list)
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }
}
