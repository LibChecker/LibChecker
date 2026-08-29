package com.absinthe.libchecker.domain.snapshot.backup.ui

import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.absinthe.libchecker.R
import com.absinthe.libchecker.ui.preference.model.PreferenceItemGroupPosition
import com.absinthe.libchecker.ui.preference.model.PreferenceItemRenderState
import com.absinthe.libchecker.ui.preference.view.PreferenceItemView
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.BottomSheetScaffoldView
import com.absinthe.libraries.utils.manager.SystemBarManager

class SnapshotBackupBottomSheetView(context: Context) : BottomSheetScaffoldView(context) {

  private var onAction: (SnapshotBackupBottomSheetAction) -> Unit = {}

  private val backupItem = buildPreferenceItem(
    key = "snapshot_backup",
    iconRes = R.drawable.ic_backup,
    titleRes = R.string.album_backup_summary,
    summaryRes = R.string.album_backup_sheet_backup_description,
    groupPosition = PreferenceItemGroupPosition.FIRST
  ) {
    onAction(SnapshotBackupBottomSheetAction.Backup)
  }

  private val restoreItem = buildPreferenceItem(
    key = "snapshot_restore",
    iconRes = R.drawable.ic_restore,
    titleRes = R.string.album_restore_from_file,
    summaryRes = R.string.album_restore_auto_detect_summary,
    groupPosition = PreferenceItemGroupPosition.LAST
  ) {
    onAction(SnapshotBackupBottomSheetAction.Restore)
  }

  private val itemContainer = LinearLayout(context).apply {
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    orientation = VERTICAL
    addView(backupItem)
    addView(restoreItem)
  }

  init {
    val padding = 16.dp
    setPadding(
      padding,
      padding,
      padding,
      (padding - SystemBarManager.navigationBarSize).coerceAtLeast(0)
    )
    header.title.text = context.getString(R.string.album_item_backup_restore_title)
    addView(itemContainer)
  }

  fun bind(onAction: (SnapshotBackupBottomSheetAction) -> Unit) {
    this.onAction = onAction
  }

  private fun buildPreferenceItem(
    key: String,
    @DrawableRes iconRes: Int,
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int,
    groupPosition: PreferenceItemGroupPosition,
    onClick: () -> Unit
  ): PreferenceItemView {
    val title = context.getString(titleRes)
    val summary = context.getString(summaryRes)
    return LayoutInflater.from(context)
      .inflate(R.layout.preference_m3e, this, false)
      .let { it as PreferenceItemView }
      .apply {
        findViewById<ImageView>(android.R.id.icon).setImageResource(iconRes)
        findViewById<TextView>(android.R.id.title).text = title
        findViewById<TextView>(android.R.id.summary).apply {
          text = summary
          isVisible = true
        }
        bind(
          PreferenceItemRenderState(
            preferenceKey = key,
            title = title,
            summary = summary,
            toggleChecked = null,
            showChevron = true,
            badgeDescription = null,
            groupPosition = groupPosition
          )
        )
        setOnClickListener { onClick() }
      }
  }
}

enum class SnapshotBackupBottomSheetAction {
  Backup,
  Restore
}
