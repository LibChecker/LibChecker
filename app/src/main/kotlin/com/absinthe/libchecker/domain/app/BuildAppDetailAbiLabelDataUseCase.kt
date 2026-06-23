package com.absinthe.libchecker.domain.app

import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.utils.PackageUtils

class BuildAppDetailAbiLabelDataUseCase {

  operator fun invoke(
    abi: Int,
    abiSet: Collection<Int>,
    apkAnalyticsMode: Boolean
  ): AppDetailAbiLabelData {
    val trueAbi = abi.mod(Constants.MULTI_ARCH)
    if (abiSet.isEmpty() || abiSet.contains(Constants.OVERLAY) || abiSet.contains(Constants.ERROR)) {
      return AppDetailAbiLabelData(
        is64Bit = PackageUtils.isAbi64Bit(trueAbi),
        labels = emptyList()
      )
    }

    val labels = buildList {
      if (abi >= Constants.MULTI_ARCH) {
        add(AppDetailAbiLabel(Constants.MULTI_ARCH, isActive = true, opensMultiArchInfo = true))
      }

      abiSet.forEach {
        if (it != Constants.NO_LIBS) {
          add(
            AppDetailAbiLabel(
              abi = it,
              isActive = apkAnalyticsMode || it == trueAbi
            )
          )
        }
      }
    }

    return AppDetailAbiLabelData(
      is64Bit = PackageUtils.isAbi64Bit(trueAbi),
      labels = labels
    )
  }
}

data class AppDetailAbiLabelData(
  val is64Bit: Boolean,
  val labels: List<AppDetailAbiLabel>
)

data class AppDetailAbiLabel(
  val abi: Int,
  val isActive: Boolean,
  val opensMultiArchInfo: Boolean = false
)
