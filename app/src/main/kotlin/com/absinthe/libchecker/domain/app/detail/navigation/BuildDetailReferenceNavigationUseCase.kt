package com.absinthe.libchecker.domain.app.detail.navigation

import com.absinthe.libchecker.annotation.ALL
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.isComponentType

class BuildDetailReferenceNavigationUseCase {

  operator fun invoke(request: DetailReferenceNavigationRequest): DetailReferenceNavigation? {
    val packageName = request.packageName ?: return null
    val refName = request.refName ?: return null
    if (request.refType == ALL) {
      return null
    }

    val tabPosition = request.visibleTypes.indexOf(request.refType)
    if (tabPosition < 0) {
      return null
    }

    return DetailReferenceNavigation(
      type = request.refType,
      tabPosition = tabPosition,
      targetName = if (isComponentType(request.refType)) {
        refName.removePrefix(packageName)
      } else {
        refName
      }
    )
  }
}

data class DetailReferenceNavigationRequest(
  val packageName: String?,
  val refName: String?,
  @LibType val refType: Int,
  val visibleTypes: List<Int>
)

data class DetailReferenceNavigation(
  @LibType val type: Int,
  val tabPosition: Int,
  val targetName: String
)
