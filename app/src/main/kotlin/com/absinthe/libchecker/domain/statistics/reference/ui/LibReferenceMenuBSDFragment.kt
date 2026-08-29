package com.absinthe.libchecker.domain.statistics.reference.ui

import android.content.DialogInterface
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.options.withOption
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceMenuAction
import com.absinthe.libchecker.domain.statistics.reference.model.buildLibReferenceMenuBottomSheetState
import com.absinthe.libchecker.domain.statistics.reference.ui.view.LibReferenceMenuBSDView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import com.google.android.material.motion.MotionUtils

class LibReferenceMenuBSDFragment : BaseBottomSheetViewDialogFragment<LibReferenceMenuBSDView>() {

  private var previousAdvancedOptions: Int = 0
  private var currentAdvancedOptions: Int = 0
  private var colorfulRuleIcon: Boolean = true

  private val demoIconPackages by lazy {
    listOf(
      PackageInfo().apply {
        packageName = requireContext().packageName
        applicationInfo = ApplicationInfo(requireContext().applicationInfo)
      }
    )
  }

  private var onDismissCallback: (optionsDiff: Int) -> Unit = {}
  private var onOptionChanged: (option: Int, isChecked: Boolean) -> Int = { option, isChecked ->
    currentAdvancedOptions.withOption(option, isChecked)
  }

  override fun initRootView(): LibReferenceMenuBSDView {
    return LibReferenceMenuBSDView(
      context = requireContext(),
      onDemoHeightAnimationStateChange = ::setExternalHeightAnimationRunning
    )
  }

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    animationDuration = MotionUtils.resolveThemeDuration(
      requireContext(),
      com.google.android.material.R.attr.motionDurationMedium3,
      DEFAULT_HEIGHT_MOTION_DURATION
    ).toLong()
    maxPeekHeightPercentage = 0.8f
    render()
    dialog?.setOnDismissListener {
      onDismissCallback(previousAdvancedOptions.xor(currentAdvancedOptions))
    }
  }

  override fun onCancel(dialog: DialogInterface) {
    super.onCancel(dialog)
    runCatching {
      dismiss()
    }
  }

  fun setOnDismissListener(action: (optionsDiff: Int) -> Unit) {
    onDismissCallback = action
  }

  fun setOptionChangeListener(
    initialOptions: Int,
    colorfulRuleIcon: Boolean,
    onOptionChanged: (option: Int, isChecked: Boolean) -> Int
  ) {
    previousAdvancedOptions = initialOptions
    currentAdvancedOptions = initialOptions
    this.colorfulRuleIcon = colorfulRuleIcon
    this.onOptionChanged = onOptionChanged
  }

  private fun render() {
    root.bind(
      state = buildLibReferenceMenuBottomSheetState(
        currentOptions = currentAdvancedOptions,
        colorfulRuleIcon = colorfulRuleIcon,
        demoIconPackages = demoIconPackages
      ),
      onAction = { action ->
        when (action) {
          is LibReferenceMenuAction.OptionChanged -> {
            currentAdvancedOptions = onOptionChanged(action.item.option, action.isChecked)
            Telemetry.recordEvent(
              Constants.Event.LIB_REF_ADVANCED_MENU_ITEM_CHANGED,
              mapOf(
                Telemetry.Param.CONTENT to getString(action.item.labelRes),
                Telemetry.Param.VALUE to action.isChecked
              )
            )
            render()
          }
        }
      }
    )
  }

  private companion object {
    const val DEFAULT_HEIGHT_MOTION_DURATION = 350
  }
}
