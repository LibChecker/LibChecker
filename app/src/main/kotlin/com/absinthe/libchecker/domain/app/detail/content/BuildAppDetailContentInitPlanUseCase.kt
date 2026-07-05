package com.absinthe.libchecker.domain.app.detail.content

import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.domain.app.model.VersionedFeature

class BuildAppDetailContentInitPlanUseCase {

  operator fun invoke(
    isHarmonyMode: Boolean,
    isApkPreview: Boolean,
    featureMask: Int
  ): AppDetailContentInitPlan {
    return AppDetailContentInitPlan(
      featureAction = if (isApkPreview) {
        AppDetailFeatureInitAction.Emit(VersionedFeature(Features.Ext.APPLICATION_PROP))
      } else {
        AppDetailFeatureInitAction.LoadPackageFeatures(featureMask)
      },
      analysisAction = when {
        isHarmonyMode -> AppDetailAnalysisInitAction.Abilities
        isApkPreview -> AppDetailAnalysisInitAction.PreviewComponents
        else -> AppDetailAnalysisInitAction.Components
      },
      shouldInitPermissions = true
    )
  }
}

data class AppDetailContentInitPlan(
  val featureAction: AppDetailFeatureInitAction,
  val analysisAction: AppDetailAnalysisInitAction,
  val shouldInitPermissions: Boolean
)

sealed interface AppDetailFeatureInitAction {
  data class Emit(val feature: VersionedFeature) : AppDetailFeatureInitAction
  data class LoadPackageFeatures(val featureMask: Int) : AppDetailFeatureInitAction
}

sealed interface AppDetailAnalysisInitAction {
  data object Components : AppDetailAnalysisInitAction
  data object PreviewComponents : AppDetailAnalysisInitAction
  data object Abilities : AppDetailAnalysisInitAction
}
