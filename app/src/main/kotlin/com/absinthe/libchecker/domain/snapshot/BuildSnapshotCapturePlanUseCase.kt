package com.absinthe.libchecker.domain.snapshot

import android.net.Uri
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.LCUris

class BuildSnapshotCapturePlanUseCase(
  private val snapshotKeep: () -> String = { GlobalValues.snapshotKeep },
  private val authKey: () -> Int = GlobalValues::generateAuthKey
) {

  operator fun invoke(selectedSnapshotTimestamp: Long): SnapshotCapturePlan {
    if (selectedSnapshotTimestamp == 0L) {
      return SnapshotCapturePlan.Capture(dropPrevious = false)
    }

    return when (snapshotKeep()) {
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

sealed interface SnapshotCapturePlan {

  data class Capture(
    val dropPrevious: Boolean
  ) : SnapshotCapturePlan

  data class ConfirmKeepPrevious(
    val bridgeUri: String
  ) : SnapshotCapturePlan

  data object NoAction : SnapshotCapturePlan
}
