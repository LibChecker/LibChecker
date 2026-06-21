package com.absinthe.libchecker.domain.statistics

import com.absinthe.libchecker.constant.Constants.MULTI_ARCH
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.ABI_32_BIT
import com.absinthe.libchecker.utils.extensions.ABI_64_BIT
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

class BuildAbiChartDataUseCase {

  suspend operator fun invoke(request: Request): AbiChartData? {
    val targets = if (request.showSystemApps) {
      request.items
    } else {
      request.items.filter { !it.isSystem }
    }
    val is64Bit = mutableListOf<LCItem>()
    val is32Bit = mutableListOf<LCItem>()
    val noNativeLibs = mutableListOf<LCItem>()
    val coroutineContext = currentCoroutineContext()

    for (item in targets) {
      if (!coroutineContext.isActive) {
        return null
      }

      if (PackageUtils.hasNoNativeLibs(item.abi.toInt())) {
        noNativeLibs.add(item)
      } else {
        when (item.abi % MULTI_ARCH) {
          in ABI_64_BIT -> is64Bit.add(item)
          in ABI_32_BIT -> is32Bit.add(item)
          else -> noNativeLibs.add(item)
        }
      }
    }

    return AbiChartData(
      is64Bit = is64Bit,
      is32Bit = is32Bit,
      noNativeLibs = noNativeLibs
    )
  }

  data class Request(
    val items: List<LCItem>,
    val showSystemApps: Boolean
  )
}

data class AbiChartData(
  val is64Bit: List<LCItem>,
  val is32Bit: List<LCItem>,
  val noNativeLibs: List<LCItem>
)
