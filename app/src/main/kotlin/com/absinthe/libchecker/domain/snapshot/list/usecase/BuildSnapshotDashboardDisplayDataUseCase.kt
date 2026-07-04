package com.absinthe.libchecker.domain.snapshot.list.usecase

import android.content.Context
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotDashboardDisplayData
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotSystemPropDisplayData
import com.absinthe.libchecker.domain.snapshot.list.model.buildSnapshotDashboardDescription
import com.absinthe.libchecker.domain.snapshot.list.model.buildSnapshotDashboardTimestampDescription
import java.util.Locale

class BuildSnapshotDashboardDisplayDataUseCase(
  private val context: Context
) {

  operator fun invoke(request: Request): SnapshotDashboardDisplayData {
    return SnapshotDashboardDisplayData(
      timestampText = request.timestampText,
      appsCountText = request.appsCountText,
      systemProps = request.systemProps,
      contentDescription = buildSnapshotDashboardDescription(
        timestampTitle = context.getString(R.string.snapshot_current_timestamp),
        timestampText = request.timestampText,
        appsCountTitle = context.getString(R.string.snapshot_apps_count),
        appsCountText = request.appsCountText
      ),
      timestampContentDescription = buildSnapshotDashboardTimestampDescription(
        actionTitle = context.getString(R.string.dialog_title_change_timestamp),
        timestampText = request.timestampText
      )
    )
  }

  fun formatAppsCount(snapshotCount: Int, appCount: Int): String {
    return String.format(Locale.getDefault(), "%d / %d", snapshotCount, appCount)
  }

  fun noSnapshotTimestampText(): String {
    return context.getString(R.string.snapshot_none)
  }

  data class Request(
    val timestampText: CharSequence,
    val appsCountText: CharSequence,
    val systemProps: List<SnapshotSystemPropDisplayData>
  )
}
