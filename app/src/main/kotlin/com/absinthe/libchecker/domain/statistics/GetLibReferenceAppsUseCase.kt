package com.absinthe.libchecker.domain.statistics

import android.content.pm.PackageManager
import com.absinthe.libchecker.annotation.ACTION
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.database.RulesRepository
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.utils.IntentFilterUtils
import com.absinthe.libchecker.utils.PackageUtils
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import timber.log.Timber

class GetLibReferenceAppsUseCase {

  suspend operator fun invoke(request: Request): Result {
    val targets = request.packageNames?.let { packageNames ->
      val packageSet = packageNames.toHashSet()
      request.items.filter { it.packageName in packageSet }
    } ?: request.items

    val filteredTargets = if (request.showSystemApps) {
      targets
    } else {
      targets.filter { !it.isSystem }
    }

    val actionTargets = mutableMapOf<String, ActionTarget>()
    val resultItems = if (request.packageNames != null) {
      if (request.type == ACTION) {
        val coroutineContext = currentCoroutineContext()
        filteredTargets.forEach { item ->
          if (!coroutineContext.isActive) {
            return Result(emptyList(), actionTargets)
          }
          resolveActionTarget(item.packageName, request.name)?.let {
            actionTargets[item.packageName] = it
          }
        }
      }
      filteredTargets
    } else {
      val coroutineContext = currentCoroutineContext()
      val result = mutableListOf<LCItem>()
      for (item in filteredTargets) {
        if (!coroutineContext.isActive) {
          return Result(result, actionTargets)
        }
        if (item.references(request.name, request.type, actionTargets)) {
          result.add(item)
        }
      }
      result
    }

    return Result(resultItems, actionTargets)
  }

  private fun LCItem.references(
    name: String,
    @LibType type: Int,
    actionTargets: MutableMap<String, ActionTarget>
  ): Boolean {
    return when (type) {
      NATIVE -> hasNativeReference(name)

      SERVICE, ACTIVITY, RECEIVER, PROVIDER -> hasComponentReference(name, type)

      PERMISSION -> hasPermissionReference(name)

      METADATA -> hasMetadataReference(name)

      ACTION -> {
        val target = resolveActionTarget(packageName, name)
        if (target != null) {
          actionTargets[packageName] = target
          true
        } else {
          false
        }
      }

      else -> false
    }
  }

  private fun LCItem.hasNativeReference(name: String): Boolean {
    return runCatching {
      val packageInfo = PackageUtils.getPackageInfo(packageName)
      PackageUtils.getNativeDirLibs(packageInfo).any {
        it.name == name && RulesRepository.checkNativeLibValidation(packageName, name)
      }
    }.onFailure {
      Timber.e(it)
    }.getOrDefault(false)
  }

  private fun LCItem.hasComponentReference(name: String, @LibType type: Int): Boolean {
    return runCatching {
      PackageUtils.getComponentStringList(packageName, type, false).contains(name)
    }.onFailure {
      Timber.e(it)
    }.getOrDefault(false)
  }

  private fun LCItem.hasPermissionReference(name: String): Boolean {
    return runCatching {
      PackageUtils.getPermissionsList(packageName).contains(name)
    }.onFailure {
      Timber.e(it)
    }.getOrDefault(false)
  }

  private fun LCItem.hasMetadataReference(name: String): Boolean {
    return runCatching {
      val packageInfo = PackageUtils.getPackageInfo(packageName, PackageManager.GET_META_DATA)
      PackageUtils.getMetaDataItems(packageInfo).any { it.name == name }
    }.onFailure {
      Timber.e(it, "Failed to retrieve package info for $packageName")
    }.getOrDefault(false)
  }

  private fun resolveActionTarget(packageName: String, actionName: String): ActionTarget? {
    val packageInfo = runCatching {
      PackageUtils.getPackageInfo(packageName, PackageManager.GET_META_DATA)
    }.onFailure {
      Timber.e(it, "Failed to retrieve package info for $packageName")
    }.getOrNull() ?: return null

    return runCatching {
      IntentFilterUtils.parseComponentsFromApk(packageInfo.applicationInfo!!.sourceDir)
        .forEach { component ->
          component.intentFilters.forEach { filter ->
            filter.actions.forEach { action ->
              if (action == actionName) {
                return ActionTarget(component.className, component.type)
              }
            }
          }
        }
      null
    }.onFailure {
      Timber.e(it, "Failed to parse intent filters for $packageName")
    }.getOrNull()
  }

  data class Request(
    val items: List<LCItem>,
    val name: String,
    @LibType val type: Int,
    val showSystemApps: Boolean,
    val packageNames: Collection<String>? = null
  )

  data class Result(
    val items: List<LCItem>,
    val actionTargets: Map<String, ActionTarget>
  )

  data class ActionTarget(
    val name: String,
    @LibType val type: Int
  )
}
