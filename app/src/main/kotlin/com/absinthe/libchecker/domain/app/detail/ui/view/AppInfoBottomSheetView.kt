package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.GridLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.AppInfoBottomSheetAction
import com.absinthe.libchecker.domain.app.detail.model.AppInfoBottomSheetState
import com.absinthe.libchecker.domain.app.detail.model.externalActions
import com.absinthe.libchecker.domain.app.detail.model.launchAction
import com.absinthe.libchecker.domain.app.detail.model.launcherActivityCopyText
import com.absinthe.libchecker.domain.app.detail.model.settingsAction
import com.absinthe.libchecker.domain.app.detail.ui.adapter.AppInfoAdapter
import com.absinthe.libchecker.ui.app.BottomSheetRecyclerView
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.setLongClickCopiedToClipboard
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import com.google.android.flexbox.FlexboxLayout

class AppInfoBottomSheetView(context: Context) :
  LinearLayout(context),
  IHeaderView {

  private var onAction: (AppInfoBottomSheetAction) -> Unit = {}

  private val actionAdapter = AppInfoAdapter { item ->
    onAction(AppInfoBottomSheetAction.OpenExternal(item))
  }

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

  private val launch = AppInfoItemView(context).apply {
    layoutParams = primaryActionLayoutParams()
    setIcon(R.drawable.ic_launch)
    setIconBackgroundTintColor(R.color.material_deep_purple_300)
    setIconTintColor(Color.WHITE)
    setText(R.string.app_info_launch)
  }

  private val setting = AppInfoItemView(context).apply {
    layoutParams = primaryActionLayoutParams()
    setIcon(R.drawable.ic_settings)
    setIconBackgroundTintColor(R.color.material_blue_grey_300)
    setIconTintColor(Color.WHITE)
    setText(R.string.app_info_settings)
  }

  private val share = AppInfoItemView(context).apply {
    layoutParams = primaryActionLayoutParams()
    setIcon(R.drawable.ic_share)
    setIconBackgroundTintColor(R.color.material_indigo_300)
    setIconTintColor(Color.WHITE)
    setText(R.string.app_info_share)
  }

  private val list = BottomSheetRecyclerView(context).apply {
    layoutParams = LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    )
    overScrollMode = OVER_SCROLL_NEVER
    adapter = actionAdapter
    layoutManager = GridLayoutManager(context, ACTION_COLUMN_COUNT)
    setHasFixedSize(true)
  }

  init {
    orientation = VERTICAL
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setPadding(24.dp, 16.dp, 24.dp, 0)
    addView(header)
    addView(functionsContainer)
    functionsContainer.addView(launch)
    functionsContainer.addView(setting)
    functionsContainer.addView(share)
    addView(list)
  }

  fun bind(
    state: AppInfoBottomSheetState,
    onAction: (AppInfoBottomSheetAction) -> Unit
  ) {
    this.onAction = onAction
    launch.apply {
      setOnClickListener { onAction(state.launchAction()) }
      setOnLongClickListener(null)
      state.launcherActivityCopyText?.let {
        setLongClickCopiedToClipboard(it)
      }
    }
    setting.setOnClickListener {
      onAction(state.settingsAction())
    }
    share.apply {
      setOnClickListener {
        onAction(AppInfoBottomSheetAction.Share(state.packageName))
      }
      setOnLongClickListener {
        onAction(AppInfoBottomSheetAction.Export(state.packageName))
        true
      }
    }
    actionAdapter.setList(state.externalActions)
  }

  override fun getHeaderView(): BottomSheetHeaderView = header

  private fun primaryActionLayoutParams(): FlexboxLayout.LayoutParams {
    return FlexboxLayout.LayoutParams(
      FlexboxLayout.LayoutParams.WRAP_CONTENT,
      FlexboxLayout.LayoutParams.WRAP_CONTENT
    ).apply {
      flexBasisPercent = 0.25f
    }
  }

  private companion object {
    const val ACTION_COLUMN_COUNT = 4
  }
}
