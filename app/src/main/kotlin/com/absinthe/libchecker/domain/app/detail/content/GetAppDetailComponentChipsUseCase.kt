package com.absinthe.libchecker.domain.app.detail.content

import android.content.pm.PackageInfo
import com.absinthe.libchecker.annotation.ACTION_IN_RULES
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.database.RulesRepository
import com.absinthe.libchecker.domain.app.detail.model.DISABLED
import com.absinthe.libchecker.domain.app.detail.model.EXPORTED
import com.absinthe.libchecker.domain.app.detail.model.LibStringItem
import com.absinthe.libchecker.domain.app.detail.model.LibStringItemChip
import com.absinthe.libchecker.features.applist.detail.bean.StatefulComponent
import com.absinthe.libchecker.utils.apk.ApkPreviewInfo
import com.absinthe.rulesbundle.Rule

class GetAppDetailComponentChipsUseCase(
  private val getAppDetailComponents: GetAppDetailComponentsUseCase
) {

  suspend operator fun invoke(
    packageInfo: PackageInfo,
    isApk: Boolean
  ): AppDetailComponentChips {
    val components = getAppDetailComponents(packageInfo, isApk)
    return components.toChips(
      packageName = packageInfo.packageName,
      useIntentFilterRules = true
    )
  }

  suspend operator fun invoke(previewInfo: ApkPreviewInfo): AppDetailComponentChips {
    return getAppDetailComponents(previewInfo).toChips(
      packageName = previewInfo.packageName,
      useIntentFilterRules = false
    )
  }

  private suspend fun AppDetailComponents.toChips(
    packageName: String,
    useIntentFilterRules: Boolean
  ): AppDetailComponentChips {
    val ruleCache = mutableMapOf<String, Rule?>()

    suspend fun getRuleCached(name: String, @LibType type: Int, regex: Boolean): Rule? {
      val key = "$type:$regex:$name"
      if (ruleCache.containsKey(key)) {
        return ruleCache[key]
      }
      return RulesRepository.getRule(name, type, regex).also {
        ruleCache[key] = it
      }
    }

    suspend fun StatefulComponent.toChip(@LibType componentType: Int): LibStringItemChip {
      var rule = if (!componentName.startsWith(".")) {
        getRuleCached(componentName, componentType, true)
      } else {
        null
      }
      if (rule == null && useIntentFilterRules) {
        val fullComponentName = if (componentName.startsWith(".")) {
          packageName + componentName
        } else {
          componentName
        }
        intentFiltersByClassName[fullComponentName]?.let { filters ->
          for (filter in filters) {
            for (action in filter.actions) {
              rule = getRuleCached(action, ACTION_IN_RULES, false)
              if (rule != null) break
            }
            if (rule != null) break
          }
        }
      }

      val source = when {
        !enabled -> DISABLED
        exported -> EXPORTED
        else -> null
      }

      return LibStringItemChip(
        LibStringItem(
          name = componentName,
          source = source,
          process = processName.takeIf { it.isNotEmpty() }
        ),
        rule
      )
    }

    return AppDetailComponentChips(
      services = services.map { it.toChip(SERVICE) },
      activities = activities.map { it.toChip(ACTIVITY) },
      receivers = receivers.map { it.toChip(RECEIVER) },
      providers = providers.map { it.toChip(PROVIDER) },
      processNames = sequenceOf(services, activities, receivers, providers)
        .flatten()
        .map { it.processName }
        .filter { it.isNotEmpty() }
        .toSet()
    )
  }
}

data class AppDetailComponentChips(
  val services: List<LibStringItemChip>,
  val activities: List<LibStringItemChip>,
  val receivers: List<LibStringItemChip>,
  val providers: List<LibStringItemChip>,
  val processNames: Set<String>
)
