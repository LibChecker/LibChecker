package com.absinthe.libchecker.appfunctions

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.database.RulesRepository
import com.absinthe.libchecker.database.entity.Features
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.detail.content.DetailContentResolver
import com.absinthe.libchecker.domain.app.detail.feature.GetAppDetailFeaturesUseCase
import com.absinthe.libchecker.domain.app.model.VersionedFeature
import com.absinthe.libchecker.domain.app.repository.InstalledAppRepository
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getPermissionsList
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.koin.android.ext.android.get

internal suspend fun BaseAppFunctionService.analyzeEvidence(
  info: PackageInfo,
  item: LCItem,
  category: String,
  installed: InstalledAppRepository
): List<EvidenceRecord> {
  if (category != "features") referenceOption(category)
  if (item.isArchived) {
    throw AppFunctionInvalidArgumentException("Archived app has no installed payload to analyze")
  }
  val results = when (category) {
    "features" -> {
      get<GetAppDetailFeaturesUseCase>()(info, cachedFeatures = -1, isApk = false).features
        .filter { it.featureType != Features.Ext.APPLICATION_ICONS }
        .map { it.toEvidence() }
    }

    "native" -> {
      val resolver = get<DetailContentResolver>()
      val libraries = resolver.getNativeLibraries(info, null, false, false, item.abi.toInt())
      libraries.itemsByAbi.flatMap { (abi, libs) ->
        currentCoroutineContext().ensureActive()
        resolver.buildChipList(info, null, false, abi, libs, false).map {
          EvidenceRecord(category, it.item.name, abi, it.rule?.label.orEmpty())
        }
      }
    }

    "permissions" -> {
      val permissions = installed.getPackageInfo(info.packageName, PackageManager.GET_PERMISSIONS)
        ?: throw AppFunctionElementNotFoundException("Package became unavailable: ${info.packageName}")
      permissions.getPermissionsList().map { EvidenceRecord(category, it, "declared", "") }
    }

    else -> {
      val type = when (category) {
        "services" -> SERVICE
        "activities" -> ACTIVITY
        "receivers" -> RECEIVER
        else -> PROVIDER
      }
      val packageInfo = installed.getPackageInfo(info.packageName, PackageUtils.getComponentPackageInfoFlag(type))
        ?: throw AppFunctionElementNotFoundException("Package became unavailable: ${info.packageName}")
      PackageUtils.getComponentStringList(packageInfo, type, false).map {
        currentCoroutineContext().ensureActive()
        EvidenceRecord(category, it, "", RulesRepository.getRule(it, type, true)?.label.orEmpty())
      }
    }
  }
  return results.sortedWith(compareBy({ it.name }, { it.value }))
}

internal fun VersionedFeature.toEvidence(): EvidenceRecord {
  val name = featureNames.entries.firstOrNull { it.value == featureType }?.key
    ?: extraFeatureNames[featureType] ?: "feature_$featureType"
  val value = if (featureType == Features.KOTLIN_USED) extras?.get("Kotlin") else version
  return EvidenceRecord("features", name, value.orEmpty(), "")
}

private val extraFeatureNames = mapOf(
  Features.Ext.APP_FUNCTION to "app_function",
  Features.Ext.APPLICATION_PROP to "application",
  Features.Ext.APPLICATION_INSTALL_SOURCE to "install_source",
  Features.Ext.ELF_PAGE_SIZE_16KB to "16kb_page_size",
  Features.Ext.ELF_PAGE_SIZE_16KB_COMPAT to "16kb_compatibility",
  Features.PLAY_SIGNING to "play_signing",
  Features.LIVE_UPDATE_NOTIFICATION to "live_update_notification"
)
