package com.absinthe.libchecker.domain.snapshot.list.ui

import androidx.core.os.bundleOf
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotDashboardAction
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.ui.preference.model.PreferenceItemGroupPosition
import com.absinthe.libchecker.view.app.PreferenceActionSheetView

class SnapshotActionsSheet : BaseBottomSheetViewDialogFragment<PreferenceActionSheetView>() {
  override fun initRootView() = PreferenceActionSheetView(requireContext(), R.string.title_snapshot).apply {
    addAction(
      key = SnapshotDashboardAction.Comparison.name,
      iconRes = R.drawable.ic_difference_two_tone,
      titleRes = R.string.album_item_comparison_title,
      summaryRes = R.string.album_item_comparison_subtitle,
      groupPosition = PreferenceItemGroupPosition.FIRST
    ) { select(SnapshotDashboardAction.Comparison) }
    addAction(
      key = SnapshotDashboardAction.Management.name,
      iconRes = R.drawable.ic_layers_two_tone,
      titleRes = R.string.album_item_management_title,
      summaryRes = R.string.album_item_management_subtitle,
      groupPosition = PreferenceItemGroupPosition.MIDDLE
    ) { select(SnapshotDashboardAction.Management) }
    addAction(
      key = SnapshotDashboardAction.BackupRestore.name,
      iconRes = R.drawable.ic_backup_two_tone,
      titleRes = R.string.album_backup,
      summaryRes = R.string.album_item_backup_restore_subtitle,
      groupPosition = PreferenceItemGroupPosition.MIDDLE
    ) { select(SnapshotDashboardAction.BackupRestore) }
    addAction(
      key = SnapshotDashboardAction.Track.name,
      iconRes = R.drawable.ic_track_two_tone,
      titleRes = R.string.album_item_track_title,
      summaryRes = R.string.album_item_track_subtitle,
      groupPosition = PreferenceItemGroupPosition.LAST
    ) { select(SnapshotDashboardAction.Track) }
  }

  override fun init() {
    maxPeekHeightPercentage = 0.82f
    isInitialLandscapeExpansionEnabled = false
  }

  private fun select(action: SnapshotDashboardAction) {
    dismiss()
    parentFragmentManager.setFragmentResult(RESULT_KEY, bundleOf(ACTION to action.name))
  }

  companion object {
    const val RESULT_KEY = "snapshot_dashboard_action"
    const val ACTION = "action"
  }
}
