package com.absinthe.libchecker.domain.app.detail.presentation.content

import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.domain.app.VersionedFeature
import com.absinthe.libchecker.domain.app.detail.content.GetAppDetailPermissionChipsUseCase
import com.absinthe.libchecker.domain.app.detail.presentation.DetailContentState
import com.absinthe.libchecker.domain.app.detail.presentation.DetailFeatureState
import com.absinthe.libchecker.domain.app.detail.presentation.DetailLoadJobsState
import com.absinthe.libchecker.domain.app.detail.presentation.DetailPackageState
import kotlinx.coroutines.CoroutineScope

class DetailPermissionContentLoader(
  private val getAppDetailPermissionChipsUseCase: GetAppDetailPermissionChipsUseCase
) {
  fun initPermissionData(
    scope: CoroutineScope,
    contentState: DetailContentState,
    featureState: DetailFeatureState,
    loadJobsState: DetailLoadJobsState,
    packageState: DetailPackageState
  ) {
    loadJobsState.launchIfNeeded(
      key = DetailLoadJobsState.Key.PERMISSIONS,
      scope = scope,
      hasData = contentState.permissionsItems.value != null
    ) {
      val permissions = getAppDetailPermissionChipsUseCase(
        packageInfo = packageState.packageInfo,
        apkPreviewInfo = packageState.apkPreviewInfo,
        isApk = packageState.isApk,
        isApkPreview = packageState.isApkPreview
      )
      contentState.permissionsItems.emit(permissions.items)

      if (permissions.hasLiveUpdateNotification) {
        featureState.emitFeature(VersionedFeature(Features.LIVE_UPDATE_NOTIFICATION))
      }
    }
  }
}
