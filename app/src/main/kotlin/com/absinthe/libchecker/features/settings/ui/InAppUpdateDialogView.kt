package com.absinthe.libchecker.features.settings.ui

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.BuildSnapshotAbiDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.display.BuildSnapshotUpdateTimeDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.features.snapshot.ui.adapter.SnapshotAdapter
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.manager.SystemBarManager
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import com.absinthe.libraries.utils.view.HeightAnimatableViewFlipper
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.loadingindicator.LoadingIndicator

class InAppUpdateDialogView(
  context: Context,
  buildSnapshotAbiDisplayData: BuildSnapshotAbiDisplayDataUseCase,
  buildSnapshotUpdateTimeDisplayData: BuildSnapshotUpdateTimeDisplayDataUseCase
) : LinearLayout(context),
  IHeaderView {

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.settings_get_updates)
  }

  val toggleGroup = MaterialButtonToggleGroup(context).apply {
    layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
      topMargin = 8.dp
    }
    isSingleSelection = true
    isSelectionRequired = true

    val styledContext = ContextThemeWrapper(context, R.style.App_Widget_AdvancedMenuToggle)
    addView(
      MaterialButton(styledContext).apply {
        id = R.id.in_app_update_chip_stable
        text = context.getString(R.string.settings_get_updates_in_app_chip_stable)
      }
    )
    addView(
      MaterialButton(styledContext).apply {
        id = R.id.in_app_update_chip_ci
        text = context.getString(R.string.settings_get_updates_in_app_chip_ci)
      }
    )
    check(R.id.in_app_update_chip_stable)
  }

  private val demoAdapter = SnapshotAdapter(
    buildSnapshotAbiDisplayData,
    buildSnapshotUpdateTimeDisplayData,
    SnapshotAdapter.CardMode.GET_APP_UPDATE
  )

  private val demoView = RecyclerView(context).apply {
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    setPadding(0, 16.dp, 0, 16.dp)
    overScrollMode = OVER_SCROLL_NEVER
    layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    adapter = demoAdapter
  }

  private val viewFlipper = HeightAnimatableViewFlipper(context).apply {
    layoutParams =
      LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    setInAnimation(context, R.anim.anim_fade_in)
    setOutAnimation(context, R.anim.anim_fade_out)
  }

  private val loading = LoadingIndicator(
    ContextThemeWrapper(context, R.style.App_Widget_M3E_LoadingIndicator_Contained)
  ).apply {
    layoutParams = FrameLayout.LayoutParams(150.dp, 150.dp).also {
      it.gravity = Gravity.CENTER
    }
  }

  val updateButton = MaterialButton(context).apply {
    layoutParams = LayoutParams(300.dp, LayoutParams.WRAP_CONTENT)
    isEnabled = false
    text = context.getString(R.string.rules_btn_update)
  }

  init {
    orientation = VERTICAL
    gravity = Gravity.CENTER_HORIZONTAL
    val padding = 16.dp
    setPadding(
      padding,
      padding,
      padding,
      (padding - SystemBarManager.navigationBarSize).coerceAtLeast(0)
    )
    addView(header)
    addView(toggleGroup)
    addView(viewFlipper)
    viewFlipper.addView(loading)
    viewFlipper.addView(demoView)
    addView(updateButton)
  }

  fun showLoading() {
    if (viewFlipper.displayedChildView != loading) {
      viewFlipper.show(loading)
    }
  }

  fun showContent() {
    if (viewFlipper.displayedChildView != demoView) {
      viewFlipper.show(demoView)
    }
  }

  fun setItem(item: SnapshotDiffItem) {
    demoAdapter.data.clear()
    demoAdapter.addData(item)
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }
}
