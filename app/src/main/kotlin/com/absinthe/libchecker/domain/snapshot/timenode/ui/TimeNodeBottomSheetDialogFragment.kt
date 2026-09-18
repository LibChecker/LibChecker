package com.absinthe.libchecker.domain.snapshot.timenode.ui

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.ContextThemeWrapper
import androidx.appcompat.app.AlertDialog
import androidx.core.os.BundleCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.snapshot.detail.ui.EXTRA_ENTITY
import com.absinthe.libchecker.domain.snapshot.detail.ui.SnapshotDetailActivity
import com.absinthe.libchecker.domain.snapshot.list.presentation.SnapshotViewModel
import com.absinthe.libchecker.domain.snapshot.timenode.model.DayContribution
import com.absinthe.libchecker.domain.snapshot.timenode.model.SnapshotUpdatedAppItem
import com.absinthe.libchecker.domain.snapshot.timenode.model.TimeNodeBottomSheetAction
import com.absinthe.libchecker.domain.snapshot.timenode.model.TimeNodeBottomSheetState
import com.absinthe.libchecker.domain.snapshot.timenode.model.TimeNodeHeaderState
import com.absinthe.libchecker.domain.snapshot.timenode.model.removeItemAt
import com.absinthe.libchecker.domain.snapshot.timenode.model.withAutoRemoveThreshold
import com.absinthe.libchecker.domain.snapshot.timenode.model.withContributionData
import com.absinthe.libchecker.domain.snapshot.timenode.model.withListData
import com.absinthe.libchecker.domain.snapshot.timenode.model.withSelectedTile
import com.absinthe.libchecker.domain.snapshot.timenode.model.withSelectedTimestamp
import com.absinthe.libchecker.domain.snapshot.timenode.ui.view.SnapshotContributionUpdatesPopup
import com.absinthe.libchecker.domain.snapshot.timenode.ui.view.TimeNodeBottomSheetView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.putArguments
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.activityViewModel

const val EXTRA_TIMESTAMP_ITEMS = "EXTRA_TIMESTAMP_ITEMS"

class TimeNodeBottomSheetDialogFragment : BaseBottomSheetViewDialogFragment<TimeNodeBottomSheetView>() {

  private val viewModel: SnapshotViewModel by activityViewModel()
  private var bindJob: Job? = null
  private var popupJob: Job? = null
  private var updatesPopup: SnapshotContributionUpdatesPopup? = null
  private var itemClickAction: ((position: Int, item: TimeStampItem) -> Unit)? = null
  private var onDismissAction: (() -> Unit)? = null
  private var addApkClickAction: ((isLeft: Boolean) -> Unit)? = null
  private var customTitle: String? = null
  private var isCompareMode: Boolean = false
  private var isLeftMode: Boolean = false
  private var dialogState: TimeNodeBottomSheetState? = null
  private var rawTimeStampItems: List<TimeStampItem> = emptyList()
  private var autoRemoveDialog: AlertDialog? = null

  override fun initRootView(): TimeNodeBottomSheetView = TimeNodeBottomSheetView(requireContext())

  override fun init() {
    maxPeekHeightPercentage = 0.75f
    isInitialLandscapeExpansionEnabled = false
    val initialSelected = arguments?.getLong(EXTRA_SELECTED_TIMESTAMP, 0L)?.takeIf { it > 0 }
    render(
      TimeNodeBottomSheetState(
        title = customTitle ?: getString(R.string.dialog_title_change_timestamp),
        selectedTimestamp = initialSelected,
        selectedDate = initialSelected?.let {
          Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        },
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
          refreshTimeStampItems(timestampItems)
        }
    }
  }

  private fun handleAction(action: TimeNodeBottomSheetAction) {
    popupJob?.cancel()
    updatesPopup?.dismissImmediately()
    when (action) {
      is TimeNodeBottomSheetAction.SelectItem -> {
        val timestamp = action.item.timestamp
        dialogState?.withSelectedTimestamp(timestamp)?.let(::render)
        root.scrollToSnapshot(timestamp)
        val timeStampItem = rawTimeStampItems.find { it.timestamp == timestamp }
          ?: TimeStampItem(timestamp, null, null)
        itemClickAction?.invoke(action.position, timeStampItem)
      }

      is TimeNodeBottomSheetAction.SelectTile -> {
        dialogState?.withSelectedTile(action.dayContribution)?.let(::render)
        showContributionUpdatesPopup(action.dayContribution, action.anchorRect)
      }

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
    val owner = viewLifecycleOwnerLiveData.value ?: return
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
      if (!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) return@createSnapshotAutoRemoveThresholdDialog
      confirmedThreshold = threshold
      updateAutoRemoveThreshold(threshold)
      recordAutoRemoveChanged(checked = true, threshold = threshold)
    }
    autoRemoveDialog = dialog
    dialog.setOnDismissListener {
      if (!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) return@setOnDismissListener
      autoRemoveDialog = null
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
    viewLifecycleOwner.lifecycleScope.launch {
      val loadingDialog = UiUtils.createLoadingDialog(context).also { it.show() }
      try {
        val timestampList = withContext(Dispatchers.IO) {
          viewModel.enableSnapshotAutoRemoveAndRetainLatest(threshold)
        }
        refreshTimeStampItems(timestampList).join()
      } finally {
        loadingDialog.dismiss()
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
    itemClickAction = { position, _ -> action(position) }
  }

  fun setOnItemClickListener(action: (position: Int, item: TimeStampItem) -> Unit) {
    itemClickAction = action
  }

  fun setOnDismissListener(action: () -> Unit) {
    onDismissAction = action
  }

  override fun onDismiss(dialog: DialogInterface) {
    super.onDismiss(dialog)
    onDismissAction?.invoke()
  }

  fun setOnAddApkClickListener(action: (isLeft: Boolean) -> Unit) {
    addApkClickAction = action
  }

  fun removeItem(position: Int) {
    val currentState = dialogState ?: return
    val updatedState = currentState.removeItemAt(position)
    if (updatedState !== currentState) {
      val timestamp = currentState.listData.items[position].timestamp
      render(updatedState)
      refreshTimeStampItems(rawTimeStampItems.filterNot { it.timestamp == timestamp })
    }
  }

  private fun refreshTimeStampItems(items: List<TimeStampItem>): Job {
    bindJob?.cancel()
    popupJob?.cancel()
    updatesPopup?.dismissImmediately()
    rawTimeStampItems = items
    return viewLifecycleOwner.lifecycleScope.launch { bindTimeStampItems(items) }.also { bindJob = it }
  }

  private suspend fun bindTimeStampItems(items: List<TimeStampItem>) {
    val selected = dialogState?.selectedTimestamp?.takeIf { timestamp -> items.any { it.timestamp == timestamp } }
      ?: items.firstOrNull()?.timestamp
    val timeNodeListData = viewModel.buildSnapshotTimeNodeListData(items).let { data ->
      if (selected != null) {
        data.copy(
          items = data.items.map { it.copy(isSelected = it.timestamp == selected) }
        )
      } else {
        data
      }
    }
    withContext(Dispatchers.Main) {
      if (context == null) {
        return@withContext
      }
      dialogState?.withSelectedTimestamp(selected)?.withListData(timeNodeListData)?.let(::render)
    }

    if (!isCompareMode) {
      val contributions = viewModel.computeSnapshotContributions(items)
      withContext(Dispatchers.Main) {
        if (context == null) {
          return@withContext
        }
        dialogState?.withContributionData(contributions)?.let(::render)
      }
    }
  }

  private fun render(state: TimeNodeBottomSheetState) {
    dialogState = state
    root.bind(state, ::handleAction)
  }

  private fun showContributionUpdatesPopup(
    dayContribution: DayContribution,
    anchorRect: Rect
  ) {
    popupJob?.cancel()
    updatesPopup?.dismissImmediately()
    popupJob = lifecycleScope.launch {
      val iconSources = if (dayContribution.updatedApps.isEmpty()) {
        emptyMap()
      } else {
        viewModel.getSnapshotPackageIconSources(dayContribution.updatedApps.map { it.packageName })
      }
      if (view == null || !isAdded || isStateSaved) return@launch
      updatesPopup = SnapshotContributionUpdatesPopup(
        host = root,
        anchorRect = anchorRect,
        dayContribution = dayContribution,
        packageIconSources = iconSources,
        onItemClick = ::navigateToSnapshotDetail
      ).also { it.show() }
    }
  }

  private fun navigateToSnapshotDetail(app: SnapshotUpdatedAppItem) {
    val ctw = context as? ContextThemeWrapper ?: activity ?: return
    lifecycleScope.launch {
      val dialog = UiUtils.createLoadingDialog(ctw)
      dialog.show()

      val diffItem = runCatching {
        withContext(Dispatchers.IO) {
          viewModel.getPackageSnapshotDiff(app.packageName, app.previousSnapshotTimestamp, app.currentSnapshotTimestamp)
        }
      }.getOrNull()

      dialog.dismiss()

      if (!isAdded || isStateSaved) {
        return@launch
      }

      if (diffItem != null) {
        val intent = Intent(ctw, SnapshotDetailActivity::class.java).apply {
          putExtras(
            Bundle().apply {
              putSerializable(EXTRA_ENTITY, diffItem)
            }
          )
        }
        startActivity(intent)
      } else {
        Toasty.showLong(ctw, R.string.toast_cant_open_app)
      }
    }
  }

  override fun onDestroyView() {
    autoRemoveDialog?.setOnDismissListener(null)
    autoRemoveDialog?.dismiss()
    autoRemoveDialog = null
    dialogState = null
    bindJob?.cancel()
    popupJob?.cancel()
    updatesPopup?.dismissImmediately()
    updatesPopup = null
    super.onDestroyView()
  }

  companion object {
    const val EXTRA_SELECTED_TIMESTAMP = "EXTRA_SELECTED_TIMESTAMP"

    fun newInstance(
      timestampItems: ArrayList<TimeStampItem>,
      selectedTimestamp: Long = 0L
    ): TimeNodeBottomSheetDialogFragment {
      return TimeNodeBottomSheetDialogFragment().putArguments(
        EXTRA_TIMESTAMP_ITEMS to timestampItems,
        EXTRA_SELECTED_TIMESTAMP to selectedTimestamp
      )
    }
  }
}
