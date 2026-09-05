package com.absinthe.libchecker.domain.statistics.reference.usecase

import android.content.pm.PackageInfo
import com.absinthe.libchecker.constant.Constants.ERROR
import com.absinthe.libchecker.constant.Constants.MULTI_ARCH
import com.absinthe.libchecker.constant.Constants.NO_LIBS
import com.absinthe.libchecker.constant.Constants.OVERLAY
import com.absinthe.libchecker.domain.app.detail.model.LibStringItem
import com.absinthe.libchecker.utils.FileUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.ABI_STRING_MAP
import java.io.File

// Pre-reuse reader retained as a differential oracle for the default ABI path.
// Keep its two-pass algorithm independent of the optimized reader.
internal object BaselineNativeLibReader {
  fun getNativeDirLibs(packageInfo: PackageInfo, specifiedAbi: Int? = null, parseElf: Boolean = false, checkCancelled: () -> Unit = {}): List<LibStringItem> {
    checkCancelled()
    val nativePath = packageInfo.applicationInfo?.nativeLibraryDir
    val result = mutableListOf<LibStringItem>()

    if (nativePath != null) {
      File(nativePath).listFiles()?.let { files ->
        val libs = files.asSequence()
          .filter {
            checkCancelled()
            it.isFile && it.extension == "so"
          }
          .distinctBy { it.name }
          .map {
            LibStringItem(
              name = it.name,
              size = FileUtils.getFileSize(it),
              elfInfo = PackageUtils.parseNativeDirElfInfo(it, parseElf, checkCancelled),
              source = it.path
            )
          }
        result.addAll(libs)
      }
    }

    if (result.isEmpty()) {
      val abi =
        specifiedAbi ?: runCatching { PackageUtils.getAbi(packageInfo) }.getOrNull() ?: return emptyList()

      if (abi == ERROR || abi == NO_LIBS || abi == OVERLAY) {
        return emptyList()
      }
      val sourceDir = ABI_STRING_MAP[abi % MULTI_ARCH]
      val libs = PackageUtils.getSourceLibs(
        packageInfo = packageInfo,
        specifiedAbi = abi,
        includeNativeLibsDir = false,
        parseElf = parseElf,
        checkCancelled = checkCancelled
      )[sourceDir] ?: emptyList()
      result.addAll(libs)
    }

    return result.distinctBy { it.name }
  }
}
