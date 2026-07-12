package com.absinthe.libchecker.domain.app.detail.model

import android.content.pm.ApplicationInfo
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.detail.related.RelatedAppDisplayData

data class OverlayDetailBottomSheetDisplay(
  val item: LCItem,
  val applicationInfo: ApplicationInfo?,
  val packageName: String,
  val appName: String?,
  val versionInfo: String,
  val extraInfo: OverlayDetailExtraInfoDisplay,
  val target: OverlayTargetPackageDisplay
)

data class OverlayDetailExtraInfoDisplay(
  val type: String,
  val targetSdkInfo: String,
  val minSdkInfo: String,
  val compileSdkInfo: String,
  val sizeInfo: String
)

sealed interface OverlayTargetPackageDisplay {
  data class RelatedApp(
    val data: RelatedAppDisplayData,
    val showHarmonyBadge: Boolean
  ) : OverlayTargetPackageDisplay

  data class PackageName(
    val value: String
  ) : OverlayTargetPackageDisplay

  data object Empty : OverlayTargetPackageDisplay
}

sealed interface OverlayDetailBottomSheetResult {
  data class Available(
    val display: OverlayDetailBottomSheetDisplay
  ) : OverlayDetailBottomSheetResult

  data object NotFound : OverlayDetailBottomSheetResult
}

sealed interface OverlayDetailAction {
  data class OpenApp(
    val item: LCItem,
    val forceDetail: Boolean
  ) : OverlayDetailAction
}
