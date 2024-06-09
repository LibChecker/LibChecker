package com.absinthe.libchecker.features.snapshot.ui.view

import android.content.Context
import android.os.Build
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.features.home.ui.adapter.AdvancedMenuAdapter
import com.absinthe.libchecker.features.snapshot.detail.bean.SnapshotDiffItem
import com.absinthe.libchecker.features.snapshot.ui.adapter.SnapshotAdapter
import com.absinthe.libchecker.ui.app.BottomSheetRecyclerView
import com.absinthe.libchecker.utils.DateUtils
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent

class SnapshotMenuBSDView(context: Context) :
  LinearLayout(context),
  IHeaderView {

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.advanced_menu)
  }

  private val adapter = AdvancedMenuAdapter()
  private val demoAdapter = SnapshotAdapter(SnapshotAdapter.CardMode.DEMO)

  private val demoView = RecyclerView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 24.dp
    }
    overScrollMode = RecyclerView.OVER_SCROLL_NEVER
    layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    adapter = demoAdapter

    demoAdapter.addData(
      SnapshotDiffItem(
        packageName = Constants.EXAMPLE_PACKAGE,
        updateTime = System.currentTimeMillis(),
        labelDiff = SnapshotDiffItem.DiffNode(DateUtils.getCurrentSeasonString(), DateUtils.getNextSeasonString()),
        versionNameDiff = SnapshotDiffItem.DiffNode("2020.3.19", DateUtils.getToday()),
        versionCodeDiff = SnapshotDiffItem.DiffNode(1120, BuildConfig.VERSION_CODE.toLong()),
        abiDiff = SnapshotDiffItem.DiffNode(Constants.ARMV7.toShort(), Constants.ARMV8.toShort()),
        targetApiDiff = SnapshotDiffItem.DiffNode((Build.VERSION.SDK_INT - 1).toShort(), Build.VERSION.SDK_INT.toShort()),
        compileSdkDiff = SnapshotDiffItem.DiffNode((Build.VERSION.SDK_INT - 1).toShort(), Build.VERSION.SDK_INT.toShort()),
        minSdkDiff = SnapshotDiffItem.DiffNode((Build.VERSION.SDK_INT - 11).toShort(), (Build.VERSION.SDK_INT - 10).toShort()),
        packageSizeDiff = SnapshotDiffItem.DiffNode(12345678L, 87654321L),
        nativeLibsDiff = SnapshotDiffItem.DiffNode(""),
        servicesDiff = SnapshotDiffItem.DiffNode(""),
        activitiesDiff = SnapshotDiffItem.DiffNode(""),
        receiversDiff = SnapshotDiffItem.DiffNode(""),
        providersDiff = SnapshotDiffItem.DiffNode(""),
        permissionsDiff = SnapshotDiffItem.DiffNode(""),
        metadataDiff = SnapshotDiffItem.DiffNode(""),
        added = true,
        removed = true,
        changed = true,
        moved = true
      )
    )
  }

  private val flexLayout = FlexboxLayout(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.topMargin = 8.dp
    }
    flexWrap = FlexWrap.WRAP
    justifyContent = JustifyContent.FLEX_START
    flexDirection = FlexDirection.ROW
  }

  private val list = BottomSheetRecyclerView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    overScrollMode = RecyclerView.OVER_SCROLL_NEVER
    adapter = this@SnapshotMenuBSDView.adapter
    layoutManager = LinearLayoutManager(context)
    isVerticalScrollBarEnabled = false
    clipToPadding = false
    clipChildren = false
    isNestedScrollingEnabled = true
    setHasFixedSize(true)

    this@SnapshotMenuBSDView.adapter.apply {
      addData(demoView)
      addData(flexLayout)
    }
  }

  fun addOptionItemView(labelRes: Int, option: Int): SnapshotMenuItemView {
    val view = SnapshotMenuItemView(context).apply {
      setOption(labelRes, option)
    }
    flexLayout.addView(view)
    return view
  }

  fun updateDemoView() {
    demoAdapter.notifyItemChanged(0)
  }

  init {
    orientation = VERTICAL
    val padding = 16.dp
    setPadding(padding, padding, padding, 0)
    addView(header)
    addView(list)
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }
}
