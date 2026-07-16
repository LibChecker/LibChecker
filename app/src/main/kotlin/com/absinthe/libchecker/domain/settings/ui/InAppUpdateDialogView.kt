package com.absinthe.libchecker.domain.settings.ui

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.update.AppUpdateChannel
import com.absinthe.libchecker.domain.settings.model.InAppUpdateDialogAction
import com.absinthe.libchecker.domain.settings.model.InAppUpdateDialogContent
import com.absinthe.libchecker.domain.settings.model.InAppUpdateDialogState
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemDisplayData
import com.absinthe.libchecker.domain.snapshot.list.ui.view.SnapshotItemView
import com.absinthe.libchecker.utils.extensions.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.manager.SystemBarManager
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import com.absinthe.libraries.utils.view.HeightAnimatableViewFlipper
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.loadingindicator.LoadingIndicator

class InAppUpdateDialogView(context: Context) :
  LinearLayout(context),
  IHeaderView {

  private var onAction: (InAppUpdateDialogAction) -> Unit = {}
  private var isBinding = false
  private var renderedItem: SnapshotItemDisplayData? = null

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.settings_get_updates)
  }

  private val toggleGroup = MaterialButtonToggleGroup(context).apply {
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
  }

  private val demoItemView = SnapshotItemView(context)

  private val demoView = FrameLayout(context).apply {
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    setPadding(0, 16.dp, 0, 16.dp)
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

  private val updateButton = MaterialButton(context).apply {
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
    toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
      if (!isBinding && isChecked) {
        checkedId.toAppUpdateChannel()?.let {
          onAction(InAppUpdateDialogAction.SelectChannel(it))
        }
      }
    }
    updateButton.setOnClickListener {
      onAction(InAppUpdateDialogAction.Update)
    }
  }

  fun bind(
    state: InAppUpdateDialogState,
    onAction: (InAppUpdateDialogAction) -> Unit
  ) {
    this.onAction = onAction
    isBinding = true
    toggleGroup.check(state.selectedChannel.toButtonId())
    isBinding = false
    doOnMainThreadIdle {
      toggleGroup.isEnabled = state.isChannelSelectionEnabled
    }
    updateButton.isEnabled = state.isUpdateEnabled

    when (val content = state.content) {
      is InAppUpdateDialogContent.Loading -> {
        setItem(content.retainedItem)
        if (viewFlipper.displayedChildView != loading) {
          viewFlipper.show(loading)
        }
      }

      is InAppUpdateDialogContent.Ready -> {
        setItem(content.item)
        if (viewFlipper.displayedChildView != demoView) {
          viewFlipper.show(demoView)
        }
      }
    }
  }

  private fun setItem(data: SnapshotItemDisplayData?) {
    if (renderedItem == data) {
      return
    }
    renderedItem = data
    demoView.removeAllViews()
    data ?: return

    demoItemView.render(data)
    demoView.addView(demoItemView)
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }

  private fun Int.toAppUpdateChannel(): AppUpdateChannel? {
    return when (this) {
      R.id.in_app_update_chip_stable -> AppUpdateChannel.STABLE
      R.id.in_app_update_chip_ci -> AppUpdateChannel.CI
      else -> null
    }
  }

  private fun AppUpdateChannel.toButtonId(): Int {
    return when (this) {
      AppUpdateChannel.STABLE -> R.id.in_app_update_chip_stable
      AppUpdateChannel.CI -> R.id.in_app_update_chip_ci
    }
  }
}
