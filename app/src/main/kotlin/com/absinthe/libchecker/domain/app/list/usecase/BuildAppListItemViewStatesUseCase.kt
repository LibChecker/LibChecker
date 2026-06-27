package com.absinthe.libchecker.domain.app.list.usecase

import android.content.Context
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.list.model.AppListItemViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BuildAppListItemViewStatesUseCase(
  private val context: Context,
  private val getAppListPackageStatesUseCase: GetAppListPackageStatesUseCase
) {

  suspend operator fun invoke(request: Request): Map<String, AppListItemViewState> = withContext(Dispatchers.IO) {
    val packageStates = getAppListPackageStatesUseCase(request.items)
    request.items.associate { item ->
      item.packageName to AppListItemViewState.create(
        context = context,
        item = item,
        packageState = packageStates.getValue(item.packageName),
        options = request.options
      )
    }
  }

  data class Request(
    val items: List<LCItem>,
    val options: Int
  )
}
