package com.absinthe.libchecker.domain.app

import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.utils.harmony.HarmonyOsUtil

class FilterAppListItemsUseCase(
  private val installedAppRepository: InstalledAppRepository
) {

  suspend operator fun invoke(request: Request): List<LCItem> {
    var filterSequence = request.items.asSequence()

    if ((request.options and AdvancedOptions.SHOW_SYSTEM_APPS) == 0) {
      filterSequence = filterSequence.filter { !it.isSystem }
    }
    if ((request.options and AdvancedOptions.SHOW_SYSTEM_FRAMEWORK_APPS) == 0) {
      filterSequence = filterSequence.filter(::shouldShowSystemFrameworkApp)
    }
    if ((request.options and AdvancedOptions.SHOW_OVERLAYS) == 0) {
      filterSequence = filterSequence.filter { it.abi.toInt() != Constants.OVERLAY }
    }
    if ((request.options and AdvancedOptions.SHOW_64_BIT_APPS) == 0) {
      filterSequence = filterSequence.filter {
        val trueAbi = it.abi.mod(Constants.MULTI_ARCH)
        it.abi.toInt() == Constants.OVERLAY ||
          !isAbi64Bit(trueAbi, request.isCurrentProcess64Bit) ||
          (trueAbi == Constants.NO_LIBS && !request.isCurrentProcess64Bit)
      }
    }
    if ((request.options and AdvancedOptions.SHOW_32_BIT_APPS) == 0) {
      filterSequence = filterSequence.filter {
        val trueAbi = it.abi.mod(Constants.MULTI_ARCH)
        it.abi.toInt() == Constants.OVERLAY ||
          isAbi64Bit(trueAbi, request.isCurrentProcess64Bit) ||
          (trueAbi == Constants.NO_LIBS && request.isCurrentProcess64Bit)
      }
    }

    if (request.keyword.isNotEmpty()) {
      filterSequence = filterSequence.filter {
        it.label.contains(request.keyword, ignoreCase = true) ||
          it.packageName.contains(request.keyword, ignoreCase = true)
      }

      if (HarmonyOsUtil.isHarmonyOs() && request.keyword.contains("Harmony", true)) {
        filterSequence = filterSequence.filter { it.variant == Constants.VARIANT_HAP }
      }
    }

    return filterSequence.toMutableList().apply {
      when {
        (request.options and AdvancedOptions.SORT_BY_NAME) > 0 -> {
          sortWith(compareBy({ it.abi }, { it.label }))
        }

        (request.options and AdvancedOptions.SORT_BY_UPDATE_TIME) > 0 -> {
          sortByDescending { it.lastUpdatedTime }
        }

        (request.options and AdvancedOptions.SORT_BY_TARGET_API) > 0 -> {
          sortByDescending { it.targetApi }
        }
      }
    }
  }

  private fun shouldShowSystemFrameworkApp(item: LCItem): Boolean {
    val isSystemFrameworkPackage = item.packageName.startsWith("com.android.") ||
      item.packageName == "android"
    return !isSystemFrameworkPackage || !installedAppRepository.isPackagePreinstalled(item.packageName)
  }

  private fun isAbi64Bit(abi: Int, isCurrentProcess64Bit: Boolean): Boolean {
    if (abi == Constants.NO_LIBS) {
      return isCurrentProcess64Bit
    }
    return abi in ABI_64_BIT
  }

  data class Request(
    val items: List<LCItem>,
    val options: Int,
    val keyword: String,
    val isCurrentProcess64Bit: Boolean
  )

  private companion object {
    val ABI_64_BIT = setOf(Constants.ARMV8, Constants.X86_64, Constants.MIPS64, Constants.RISCV64)
  }
}
