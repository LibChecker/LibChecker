package com.absinthe.libchecker.domain.snapshot.list.usecase

import android.net.Uri
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.LCUris
import com.absinthe.libchecker.domain.snapshot.SnapshotSettingsRepository
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotCapturePlan

class BuildSnapshotCapturePlanUseCase(
  private val snapshotSettingsRepository: SnapshotSettingsRepository,
  private val authKey: () -> Int = GlobalValues::generateAuthKey
) {

  operator fun invoke(selectedSnapshotTimestamp: Long): SnapshotCapturePlan {
    if (selectedSnapshotTimestamp == 0L) {
      return SnapshotCapturePlan.Capture(dropPrevious = false)
    }

    return when (snapshotSettingsRepository.keepRule) {
      Constants.SNAPSHOT_DEFAULT -> SnapshotCapturePlan.ConfirmKeepPrevious(
        bridgeUri = buildShootBridgeUri(dropPrevious = false)
      )

      Constants.SNAPSHOT_KEEP -> SnapshotCapturePlan.Capture(dropPrevious = false)

      Constants.SNAPSHOT_DISCARD -> SnapshotCapturePlan.Capture(dropPrevious = true)

      else -> SnapshotCapturePlan.NoAction
    }
  }

  private fun buildShootBridgeUri(dropPrevious: Boolean): String {
    return Uri.Builder().scheme(LCUris.SCHEME)
      .authority(LCUris.Bridge.AUTHORITY)
      .appendQueryParameter(LCUris.Bridge.PARAM_ACTION, LCUris.Bridge.ACTION_SHOOT)
      .appendQueryParameter(LCUris.Bridge.PARAM_AUTHORITY, authKey().toString())
      .appendQueryParameter(LCUris.Bridge.PARAM_DROP_PREVIOUS, dropPrevious.toString())
      .build()
      .toString()
  }
}
