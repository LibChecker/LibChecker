package com.absinthe.libchecker.domain.app.detail.abi

import android.content.pm.PackageInfo
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.apk.ApkPreviewInfo
import java.io.File

class GetAppDetailAbiUseCase {

  operator fun invoke(packageInfo: PackageInfo, isApk: Boolean): AppDetailAbi? {
    val source = runCatching { packageInfo.applicationInfo?.sourceDir }.getOrNull() ?: return null
    val abiSet = PackageUtils.getAbiSet(
      file = File(source),
      packageInfo = packageInfo,
      isApk = isApk,
      ignoreArch = true
    ).toSet()
    val abi = PackageUtils.getAbi(packageInfo, isApk = isApk, abiSet = abiSet)
    return AppDetailAbi(
      abi = abi,
      abiSet = abiSet.sortedForDetail(abi)
    )
  }

  operator fun invoke(apkPreviewInfo: ApkPreviewInfo): AppDetailAbi? {
    val abiSet = apkPreviewInfo.abiSet
    if (abiSet.isEmpty()) return null
    val abi = abiSet.first()
    return AppDetailAbi(
      abi = abi,
      abiSet = abiSet.sortedForDetail(abi)
    )
  }

  private fun Collection<Int>.sortedForDetail(abi: Int): List<Int> {
    return sortedByDescending {
      it == abi || PackageUtils.isAbi64Bit(it)
    }
  }
}
