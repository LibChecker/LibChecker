package com.absinthe.libchecker.domain.statistics.chart.usecase

import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.database.entity.LCItem
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

class BuildFeatureFlagChartDataUseCase {

  suspend operator fun invoke(request: Request): FeatureFlagChartData? {
    val targets = if (request.showSystemApps) {
      request.items
    } else {
      request.items.filter { !it.isSystem }
    }
    val matched = mutableListOf<LCItem>()
    val unmatched = mutableListOf<LCItem>()
    val coroutineContext = currentCoroutineContext()

    for (item in targets) {
      if (!coroutineContext.isActive) {
        return null
      }

      if ((item.features and request.kind.featureFlag) > 0) {
        matched.add(item)
      } else {
        unmatched.add(item)
      }
    }

    return FeatureFlagChartData(
      matched = matched,
      unmatched = unmatched
    )
  }

  data class Request(
    val items: List<LCItem>,
    val kind: Kind,
    val showSystemApps: Boolean
  )

  enum class Kind(val featureFlag: Int) {
    Kotlin(Features.KOTLIN_USED),
    JetpackCompose(Features.JETPACK_COMPOSE),
    AppBundle(Features.SPLIT_APKS)
  }
}

data class FeatureFlagChartData(
  val matched: List<LCItem>,
  val unmatched: List<LCItem>
)
