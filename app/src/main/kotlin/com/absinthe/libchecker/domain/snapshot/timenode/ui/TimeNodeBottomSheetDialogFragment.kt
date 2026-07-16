package com.absinthe.libchecker.domain.snapshot.timenode.ui

import android.view.ContextThemeWrapper
import androidx.appcompat.app.AlertDialog
import androidx.core.os.BundleCompat
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.snapshot.list.presentation.SnapshotViewModel
import com.absinthe.libchecker.domain.snapshot.timenode.model.TimeNodeBottomSheetAction
import com.absinthe.libchecker.domain.snapshot.timenode.model.TimeNodeBottomSheetState
import com.absinthe.libchecker.domain.snapshot.timenode.model.TimeNodeHeaderState
import com.absinthe.libchecker.domain.snapshot.timenode.model.removeItemAt
import com.absinthe.libchecker.domain.snapshot.timenode.model.withAutoRemoveThreshold
import com.absinthe.libchecker.domain.snapshot.timenode.model.withListData
import com.absinthe.libchecker.domain.snapshot.timenode.ui.view.TimeNodeBottomSheetView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.activityViewModel

const val EXTRA_TIMESTAMP_ITEMS = "EXTRA_TIMESTAMP_ITEMS"

class TimeNodeBottomSheetDialogFragment : BaseBottomSheetViewDialogFragment<TimeNodeBottomSheetView>() {

  private val viewModel: SnapshotViewModel by activityViewModel()
  private var itemClickAction: ((position: Int) -> Unit)? = null
  private var addApkClickAction: ((isLeft: Boolean) -> Unit)? = null
  private var customTitle: String? = null
  private var isCompareMode: Boolean = false
  private var isLeftMode: Boolean = false
  private var dialogState: TimeNodeBottomSheetState? = null

  override fun initRootView(): TimeNodeBottomSheetView = TimeNodeBottomSheetView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    maxPeekHeightPercentage = 0.67f
    isInitialLandscapeExpansionEnabled = false
    render(
      TimeNodeBottomSheetState(
        title = customTitle ?: getString(R.string.dialog_title_change_timestamp),
        header = if (isCompareMode) {
          TimeNodeHeaderState.AddApk(isLeftMode)
        } else {
          TimeNodeHeaderState.AutoRemove(viewModel.getSnapshotAutoRemoveThreshold())
        }
      )
    )

    arguments?.let {
      BundleCompat.getParcelableArrayList(it, EXTRA_TIMESTAMP_ITEMS, TimeStampItem::class.java)
        ?.let { timestampItems ->
          lifecycleScope.launch {
            bindTimeStampItems(timestampItems)
          }
        }
    }
  }

  private fun handleAction(action: TimeNodeBottomSheetAction) {
    when (action) {
      is TimeNodeBottomSheetAction.SelectItem -> itemClickAction?.invoke(action.position)

      is TimeNodeBottomSheetAction.AddApk -> {
        addApkClickAction?.invoke(action.isLeft)
        dismiss()
      }

      is TimeNodeBottomSheetAction.SetAutoRemoveEnabled -> {
        if (action.enabled) {
          showAutoRemoveThresholdDialog()
        } else {
          viewModel.disableSnapshotAutoRemoveThreshold()
          updateAutoRemoveThreshold(-1)
          recordAutoRemoveChanged(checked = false, threshold = -1)
        }
      }
    }
  }

  private fun showAutoRemoveThresholdDialog() {
    val ctw = context as? ContextThemeWrapper
    if (ctw == null) {
      updateAutoRemoveThreshold(viewModel.getSnapshotAutoRemoveThreshold())
      return
    }

    var confirmedThreshold: Int? = null
    val dialog = UiUtils.createSnapshotAutoRemoveThresholdDialog(
      ctw,
      viewModel.getSnapshotAutoRemoveThreshold()
    ) { threshold ->
      confirmedThreshold = threshold
      updateAutoRemoveThreshold(threshold)
      recordAutoRemoveChanged(checked = true, threshold = threshold)
    }
    dialog.setOnDismissListener {
      val threshold = confirmedThreshold
      if (threshold == null) {
        updateAutoRemoveThreshold(viewModel.getSnapshotAutoRemoveThreshold())
      } else {
        retainLatestSnapshots(ctw, threshold)
      }
    }
    dialog.show()
  }

  private fun retainLatestSnapshots(
    context: ContextThemeWrapper,
    threshold: Int
  ) {
    lifecycleScope.launch(Dispatchers.IO) {
      var loadingDialog: AlertDialog? = null
      try {
        withContext(Dispatchers.Main) {
          loadingDialog = UiUtils.createLoadingDialog(context).also { it.show() }
        }
        val timestampList = viewModel.enableSnapshotAutoRemoveAndRetainLatest(threshold)
        bindTimeStampItems(timestampList)
      } finally {
        withContext(Dispatchers.Main) {
          loadingDialog?.dismiss()
        }
      }
    }
  }

  private fun updateAutoRemoveThreshold(threshold: Int) {
    dialogState?.withAutoRemoveThreshold(threshold)?.let(::render)
  }

  private fun recordAutoRemoveChanged(
    checked: Boolean,
    threshold: Int
  ) {
    val content = if (threshold <= 0) {
      getString(R.string.album_item_management_snapshot_auto_remove_default_title)
    } else {
      getString(
        R.string.album_item_management_snapshot_auto_remove_specific_title,
        threshold
      )
    }
    Telemetry.recordEvent(
      Constants.Event.SNAPSHOT_ADVANCED_MENU_ITEM_CHANGED,
      mapOf(
        Telemetry.Param.CONTENT to content,
        Telemetry.Param.VALUE to checked
      )
    )
  }

  fun setTitle(title: String) {
    customTitle = title
    dialogState?.copy(title = title)?.let(::render)
  }

  fun setCompareMode(isCompareMode: Boolean) {
    this.isCompareMode = isCompareMode
  }

  fun setLeftMode(isLeftMode: Boolean) {
    this.isLeftMode = isLeftMode
  }

  fun setOnItemClickListener(action: (position: Int) -> Unit) {
    itemClickAction = action
  }

  fun setOnAddApkClickListener(action: (isLeft: Boolean) -> Unit) {
    addApkClickAction = action
  }

  fun removeItem(position: Int) {
    val currentState = dialogState ?: return
    val updatedState = currentState.removeItemAt(position)
    if (updatedState !== currentState) {
      render(updatedState)
    }
  }

  private suspend fun bindTimeStampItems(items: List<TimeStampItem>) {
    val timeNodeListData = viewModel.buildSnapshotTimeNodeListData(items)
    withContext(Dispatchers.Main) {
      if (context == null) {
        return@withContext
      }
      dialogState?.withListData(timeNodeListData)?.let(::render)
    }
  }

  private fun render(state: TimeNodeBottomSheetState) {
    dialogState = state
    root.bind(state, ::handleAction)
  }

  companion object {
    fun newInstance(timestampItems: ArrayList<TimeStampItem>): TimeNodeBottomSheetDialogFragment {
      return TimeNodeBottomSheetDialogFragment().putArguments(
        EXTRA_TIMESTAMP_ITEMS to timestampItems
      )
    }
  }
}
