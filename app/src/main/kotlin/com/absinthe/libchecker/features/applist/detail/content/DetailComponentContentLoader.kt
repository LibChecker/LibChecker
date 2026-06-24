package com.absinthe.libchecker.features.applist.detail.content

import com.absinthe.libchecker.domain.app.detail.GetAppDetailComponentChipsUseCase
import com.absinthe.libchecker.features.applist.detail.DetailContentState
import com.absinthe.libchecker.features.applist.detail.DetailLoadJobsState
import com.absinthe.libchecker.features.applist.detail.DetailPackageState
import com.absinthe.libchecker.utils.UiUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class DetailComponentContentLoader(
  private val getAppDetailComponentChipsUseCase: GetAppDetailComponentChipsUseCase
) {
  fun initComponentsData(
    scope: CoroutineScope,
    contentState: DetailContentState,
    loadJobsState: DetailLoadJobsState,
    packageState: DetailPackageState
  ) {
    loadJobsState.launchIfNeeded(
      key = DetailLoadJobsState.Key.COMPONENTS,
      scope = scope,
      hasData = contentState.hasComponentsData()
    ) {
      try {
        val components = getAppDetailComponentChipsUseCase(
          packageState.packageInfo,
          packageState.isApk
        )
        contentState.emitComponents(components) { UiUtils.getRandomColor() }
      } catch (e: Exception) {
        Timber.e(e)
      }
    }
  }

  fun initComponentsDataInPreview(
    scope: CoroutineScope,
    contentState: DetailContentState,
    packageState: DetailPackageState
  ) = scope.launch(Dispatchers.IO) {
    val previewInfo = packageState.apkPreviewInfo ?: return@launch
    val components = getAppDetailComponentChipsUseCase(previewInfo)
    contentState.emitComponentItems(components)
  }
}
