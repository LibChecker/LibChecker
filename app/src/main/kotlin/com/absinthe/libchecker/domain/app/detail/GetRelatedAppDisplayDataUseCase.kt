package com.absinthe.libchecker.domain.app.detail

import com.absinthe.libchecker.domain.app.list.related.GetRelatedAppListItemUseCase

class GetRelatedAppDisplayDataUseCase(
  private val getRelatedAppListItemUseCase: GetRelatedAppListItemUseCase,
  private val buildRelatedAppDisplayDataUseCase: BuildRelatedAppDisplayDataUseCase
) {

  suspend operator fun invoke(packageName: String): RelatedAppDisplayData? {
    val relatedApp = getRelatedAppListItemUseCase(packageName) ?: return null
    return buildRelatedAppDisplayDataUseCase(packageName, relatedApp)
  }
}
