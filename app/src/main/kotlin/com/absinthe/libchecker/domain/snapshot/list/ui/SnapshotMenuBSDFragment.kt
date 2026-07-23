package com.absinthe.libchecker.domain.snapshot.list.ui

import android.content.DialogInterface
import android.os.Build
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.options.SnapshotOptions
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemCardPresentation
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemDisplayData
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotMenuAction
import com.absinthe.libchecker.domain.snapshot.list.model.buildSnapshotMenuBottomSheetState
import com.absinthe.libchecker.domain.snapshot.list.presentation.SnapshotViewModel
import com.absinthe.libchecker.domain.snapshot.list.ui.view.SnapshotMenuBSDView
import com.absinthe.libchecker.domain.snapshot.list.usecase.BuildSnapshotItemDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.DateUtils
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.supportIECUnit
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class SnapshotMenuBSDFragment : BaseBottomSheetViewDialogFragment<SnapshotMenuBSDView>() {

  private val viewModel: SnapshotViewModel by activityViewModel()
  private val buildSnapshotItemDisplayData: BuildSnapshotItemDisplayDataUseCase by inject()
  private val demoItem by lazy(LazyThreadSafetyMode.NONE, ::buildDemoItem)
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
        emphasizeDiffs = (options and SnapshotOptions.DIFF_EMPHASIS) > 0,
        highlightText = ""
      )
    )
  }

  private fun buildDemoItem(): SnapshotDiffItem {
    val sdkInt = Build.VERSION.SDK_INT
    return SnapshotDiffItem(
      packageName = Constants.EXAMPLE_PACKAGE,
      updateTime = System.currentTimeMillis(),
      labelDiff = SnapshotDiffItem.DiffNode(DateUtils.getCurrentSeasonString(), DateUtils.getNextSeasonString()),
      versionNameDiff = SnapshotDiffItem.DiffNode("2020.3.19", DateUtils.getToday()),
      versionCodeDiff = SnapshotDiffItem.DiffNode(1120, BuildConfig.VERSION_CODE.toLong()),
      abiDiff = SnapshotDiffItem.DiffNode(Constants.ARMV7.toShort(), Constants.ARMV8.toShort()),
      targetApiDiff = SnapshotDiffItem.DiffNode((sdkInt - 1).toShort(), sdkInt.toShort()),
      compileSdkDiff = SnapshotDiffItem.DiffNode((sdkInt - 1).toShort(), sdkInt.toShort()),
      minSdkDiff = SnapshotDiffItem.DiffNode((sdkInt - 11).toShort(), (sdkInt - 10).toShort()),
      packageSizeDiff = SnapshotDiffItem.DiffNode(12345678L, 87654321L),
      nativeLibsDiff = SnapshotDiffItem.DiffNode(""),
      servicesDiff = SnapshotDiffItem.DiffNode(""),
      activitiesDiff = SnapshotDiffItem.DiffNode(""),
      receiversDiff = SnapshotDiffItem.DiffNode(""),
      providersDiff = SnapshotDiffItem.DiffNode(""),
      permissionsDiff = SnapshotDiffItem.DiffNode(""),
      metadataDiff = SnapshotDiffItem.DiffNode(""),
      added = 100,
      removed = 100,
      changed = 100,
      moved = 100
    )
  }
}
