package com.absinthe.libchecker.utils.dex

import android.content.pm.PackageInfo
import android.os.Build
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.ShizukuManager
import hidden.DexFileHidden
import timber.log.Timber

object DexOptimizationReader {
  fun PackageInfo.getDexFileOptimizationInfo(): DexFileOptimizationInfo? {
    val sourceDir = applicationInfo?.sourceDir ?: return null
    return getDexFileOptimizationInfoFromPackageDump(sourceDir)
      ?: getDexFileOptimizationInfoFromDexFile(sourceDir)
  }

  data class DexFileOptimizationInfo(
    val status: String,
    val reason: String,
    val instructionSet: String? = null
  )

  private data class DexoptDumpInfo(
    val instructionSet: String,
    val status: String,
    val reason: String,
    val isPrimaryAbi: Boolean
  )

  private fun PackageInfo.getDexFileOptimizationInfoFromPackageDump(sourceDir: String): DexFileOptimizationInfo? {
    if (!ShizukuManager.requireAvailable()) {
      return null
    }

    return runCatching {
      ShizukuManager.dumpPackageService(arrayOf(packageName))
    }.onFailure {
      Timber.w(it, "getDexFileOptimizationInfo: package dump failed")
    }.getOrNull()?.let { dump ->
      parseDexoptDump(dump, packageName, sourceDir)
    }?.also {
      Timber.d("getDexFileOptimizationInfo: status=${it.status}, reason=${it.reason}, isa=${it.instructionSet}")
    }
  }

  private fun PackageInfo.getDexFileOptimizationInfoFromDexFile(sourceDir: String): DexFileOptimizationInfo? {
    if (!OsUtils.atLeastP()) {
      return null
    }
    val instructionSet = ABI_TO_INSTRUCTION_SET_MAP[Build.SUPPORTED_ABIS.firstOrNull()] ?: return null
    return runCatching {
      val info = DexFileHidden.getDexFileOptimizationInfo(sourceDir, instructionSet)
      DexFileOptimizationInfo(info.status, info.reason, instructionSet)
    }.onFailure {
      Timber.w(it, "getDexFileOptimizationInfo: DexFile fallback failed")
    }.getOrNull()?.also {
      Timber.d("getDexFileOptimizationInfo: fallback status=${it.status}, reason=${it.reason}, isa=${it.instructionSet}")
    }
  }

  private fun parseDexoptDump(
    dump: String,
    packageName: String,
    sourceDir: String
  ): DexFileOptimizationInfo? {
    val entries = mutableListOf<DexoptDumpInfo>()
    var inDexoptState = false
    var inTargetPackage = false
    var inTargetPath = false

    for (line in dump.lineSequence()) {
      val value = line.trim()
      when {
        value == "Dexopt state:" -> {
          inDexoptState = true
          inTargetPackage = false
          inTargetPath = false
        }

        inDexoptState && value == "[$packageName]" -> {
          inTargetPackage = true
          inTargetPath = false
        }

        inTargetPackage && value.startsWith("[") && value.endsWith("]") -> {
          if (entries.isNotEmpty()) {
            break
          }
          inTargetPackage = false
          inTargetPath = false
        }

        inTargetPackage && value.startsWith("path: ") -> {
          if (entries.isNotEmpty()) {
            break
          }
          inTargetPath = value.removePrefix("path: ").substringBefore(' ') == sourceDir
        }

        inTargetPackage && inTargetPath -> {
          val match = DEXOPT_STATUS_PATTERN.matchEntire(value) ?: continue
          entries += DexoptDumpInfo(
            instructionSet = match.groupValues[1],
            status = match.groupValues[2],
            reason = match.groupValues[3],
            isPrimaryAbi = match.groupValues[4].contains("[primary-abi]")
          )
        }
      }
    }

    val preferredInstructionSet = ABI_TO_INSTRUCTION_SET_MAP[Build.SUPPORTED_ABIS.firstOrNull()]
    val entry = entries.firstOrNull { it.isPrimaryAbi }
      ?: entries.firstOrNull { it.instructionSet == preferredInstructionSet }
      ?: entries.firstOrNull()
    return entry?.let {
      DexFileOptimizationInfo(it.status, it.reason, it.instructionSet)
    }
  }

  private val DEXOPT_STATUS_PATTERN = Regex("""^(\S+):\s+\[status=([^\]]+)]\s+\[reason=([^\]]+)](.*)$""")

  // Keep in sync with `ABI_TO_INSTRUCTION_SET_MAP` in
  // libcore/libart/src/main/java/dalvik/system/VMRuntime.java.
  private val ABI_TO_INSTRUCTION_SET_MAP = mapOf(
    "armeabi" to "arm",
    "armeabi-v7a" to "arm",
    "x86" to "x86",
    "x86_64" to "x86_64",
    "arm64-v8a" to "arm64",
    "arm64-v8a-hwasan" to "arm64",
    "riscv64" to "riscv64"
  )
}
