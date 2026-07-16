package com.absinthe.libchecker.domain.app.list.usecase

import android.content.Context
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.list.TRACE_APP_LIST_CREATE_ITEM_VIEW_STATES
import com.absinthe.libchecker.domain.app.list.TRACE_APP_LIST_ITEM_VIEW_STATES
import com.absinthe.libchecker.domain.app.list.TRACE_APP_LIST_PACKAGE_STATES
import com.absinthe.libchecker.domain.app.list.model.AppListItemViewState
import com.absinthe.libchecker.domain.app.list.traceAppListSection
import com.absinthe.libchecker.domain.app.list.traceAppListSuspendSection
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BuildAppListItemViewStatesUseCase(
  private val context: Context,
  private val getAppListPackageStatesUseCase: GetAppListPackageStatesUseCase,
  private val installedAppRepository: InstalledAppRepository
) {

  fun createPackageStateSnapshot(): GetAppListPackageStatesUseCase.PackageStateSnapshot {
    return getAppListPackageStatesUseCase.createSnapshot()
  }

  suspend operator fun invoke(request: Request): Map<String, AppListItemViewState> = withContext(Dispatchers.IO) {
    traceAppListSuspendSection(TRACE_APP_LIST_ITEM_VIEW_STATES) {
      val packageStates = traceAppListSuspendSection(TRACE_APP_LIST_PACKAGE_STATES) {
        getAppListPackageStatesUseCase(request.items, request.packageStateSnapshot)
      }
      val apexPackageNames = installedAppRepository.getApexPackageNames()
      traceAppListSection(TRACE_APP_LIST_CREATE_ITEM_VIEW_STATES) {
        request.items.associate { item ->
          item.packageName to AppListItemViewState.create(
            context = context,
            item = item,
            packageState = packageStates.getValue(item.packageName),
            options = request.options,
            isApexPackage = item.packageName in apexPackageNames
          )
        }
      }
    }
  }

  data class Request(
    val items: List<LCItem>,
    val options: Int,
    val packageStateSnapshot: GetAppListPackageStatesUseCase.PackageStateSnapshot? = null
  )
}
