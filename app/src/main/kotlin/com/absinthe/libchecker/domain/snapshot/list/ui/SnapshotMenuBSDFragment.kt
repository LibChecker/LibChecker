package com.absinthe.libchecker.domain.snapshot.list.ui

import android.content.DialogInterface
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.options.SnapshotOptions
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemCardPresentation
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemDisplayData
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotMenuAction
import com.absinthe.libchecker.domain.snapshot.list.model.buildSnapshotMenuBottomSheetState
import com.absinthe.libchecker.domain.snapshot.list.presentation.SnapshotViewModel
import com.absinthe.libchecker.domain.snapshot.list.ui.view.SnapshotMenuBSDView
import com.absinthe.libchecker.domain.snapshot.list.usecase.BuildSnapshotItemDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.list.usecase.BuildSnapshotMenuDemoItemUseCase
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.supportIECUnit
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class SnapshotMenuBSDFragment : BaseBottomSheetViewDialogFragment<SnapshotMenuBSDView>() {

  private val viewModel: SnapshotViewModel by activityViewModel()
  private val buildSnapshotItemDisplayData: BuildSnapshotItemDisplayDataUseCase by inject()
  private val buildSnapshotMenuDemoItem: BuildSnapshotMenuDemoItemUseCase by inject()
  private val demoItem by lazy(LazyThreadSafetyMode.NONE) {
    buildSnapshotMenuDemoItem()
  }
  private var previousAdvancedOptions: Int = 0

  private var onDismissCallback: (optionsDiff: Int) -> Unit = {}

  override fun initRootView(): SnapshotMenuBSDView = SnapshotMenuBSDView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    maxPeekHeightPercentage = 0.8f
    previousAdvancedOptions = viewModel.getSnapshotOptions()
    render(previousAdvancedOptions)
    dialog?.setOnDismissListener {
      onDismissCallback(viewModel.getSnapshotOptionsDiff(previousAdvancedOptions))
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

  private fun render(options: Int = viewModel.getSnapshotOptions()) {
    root.bind(
      state = buildSnapshotMenuBottomSheetState(
        currentOptions = options,
        demoDisplayData = buildDemoDisplayData(options),
        includeIecUnits = supportIECUnit
      ),
      onAction = ::handleAction
    )
  }

  private fun handleAction(action: SnapshotMenuAction) {
    when (action) {
      is SnapshotMenuAction.OptionChanged -> {
        val currentOptions = updateOption(
          labelRes = action.item.labelRes,
          option = action.item.option,
          isChecked = action.isChecked
        )
        render(currentOptions)
      }
    }
  }

  private fun updateOption(labelRes: Int, option: Int, isChecked: Boolean): Int {
    val currentOptions = viewModel.setSnapshotOption(option, isChecked)
    Telemetry.recordEvent(
      Constants.Event.SNAPSHOT_ADVANCED_MENU_ITEM_CHANGED,
      mapOf(Telemetry.Param.CONTENT to getString(labelRes), Telemetry.Param.VALUE to isChecked)
    )
    return currentOptions
  }

  private fun buildDemoDisplayData(options: Int): SnapshotItemDisplayData {
    return buildSnapshotItemDisplayData(
      BuildSnapshotItemDisplayDataUseCase.Request(
        item = demoItem,
        cardPresentation = SnapshotItemCardPresentation.Rounded,
        iconSource = null,
        showUpdateTime = (options and SnapshotOptions.SHOW_UPDATE_TIME) > 0,
        isApexPackage = false,
        animateStateIndicator = true,
        tintChangedAbiBadge = false,
        highlightDiffColor = if ((options and SnapshotOptions.DIFF_HIGHLIGHT) > 0) {
          requireContext().getColorByAttr(androidx.appcompat.R.attr.colorPrimary)
        } else {
          null
        },
        highlightText = ""
      )
    )
  }
}
