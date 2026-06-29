package com.absinthe.libchecker.domain.app.detail

import android.content.Context
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.utils.PackageUtils

class BuildAppDetailAbiLabelDataUseCase(
  private val context: Context
) {

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
        add(
          buildLabel(
            abi = Constants.MULTI_ARCH,
            isActive = true,
            opensMultiArchInfo = true
          )
        )
      }

      abiSet.forEach {
        if (it != Constants.NO_LIBS) {
          add(
            buildLabel(
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

  private fun buildLabel(
    abi: Int,
    isActive: Boolean,
    opensMultiArchInfo: Boolean = false
  ): AppDetailAbiLabel {
    return AppDetailAbiLabel(
      abi = abi,
      isActive = isActive,
      contentDescription = if (abi == Constants.MULTI_ARCH) {
        context.getString(R.string.multiArch)
      } else {
        PackageUtils.getAbiString(context, abi, showExtraInfo = false)
      },
      is64Bit = PackageUtils.isAbi64Bit(abi),
      opensMultiArchInfo = opensMultiArchInfo
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
  val contentDescription: String,
  val is64Bit: Boolean,
  val opensMultiArchInfo: Boolean = false
)
