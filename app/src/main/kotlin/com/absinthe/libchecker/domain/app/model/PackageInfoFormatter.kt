package com.absinthe.libchecker.domain.app.model

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.collection.arrayMapOf
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ET_CORE
import com.absinthe.libchecker.annotation.ET_DYN
import com.absinthe.libchecker.annotation.ET_EXEC
import com.absinthe.libchecker.annotation.ET_HIPROC
import com.absinthe.libchecker.annotation.ET_LOPROC
import com.absinthe.libchecker.annotation.ET_NONE
import com.absinthe.libchecker.annotation.ET_NOT_ELF
import com.absinthe.libchecker.annotation.ET_NOT_SET
import com.absinthe.libchecker.annotation.ET_REL
import com.absinthe.libchecker.annotation.ElfType
import com.absinthe.libchecker.constant.AndroidVersions
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.Constants.ARMV5
import com.absinthe.libchecker.constant.Constants.ARMV7
import com.absinthe.libchecker.constant.Constants.ARMV8
import com.absinthe.libchecker.constant.Constants.ERROR
import com.absinthe.libchecker.constant.Constants.MIPS
import com.absinthe.libchecker.constant.Constants.MIPS64
import com.absinthe.libchecker.constant.Constants.MULTI_ARCH
import com.absinthe.libchecker.constant.Constants.NO_LIBS
import com.absinthe.libchecker.constant.Constants.OVERLAY
import com.absinthe.libchecker.constant.Constants.RISCV32
import com.absinthe.libchecker.constant.Constants.RISCV64
import com.absinthe.libchecker.constant.Constants.X86
import com.absinthe.libchecker.constant.Constants.X86_64
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.domain.app.detail.model.LibStringItem
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.elf.NativeLibraryAnalyzer.isAbi64Bit
import com.absinthe.libchecker.utils.extensions.getCompileSdkVersion
import com.absinthe.libchecker.utils.extensions.sizeToString
import java.io.File

object PackageInfoFormatter {
  private val UNKNOWN_ABI_RES = listOf(R.string.unknown)

  /**
   * Get ABI string from ABI type
   * @param context Context
   * @param abi ABI type
   * @param showExtraInfo show "multiArch" etc. if is true
   * @return ABI string
   */
  fun getAbiString(context: Context, abi: Int, showExtraInfo: Boolean): String {
    if (abi == OVERLAY) {
      return Constants.OVERLAY_STRING
    }
    val resList = if (!showExtraInfo && abi >= MULTI_ARCH) {
      ABI_STRING_RES_MAP[abi % MULTI_ARCH] ?: UNKNOWN_ABI_RES
    } else {
      ABI_STRING_RES_MAP[abi] ?: UNKNOWN_ABI_RES
    }
    return resList.joinToString { context.getString(it) }
  }

  /**
   * Get ABI badge resource from ABI type
   * @param type ABI type
   * @return Badge resource
   */
  @DrawableRes
  fun getAbiBadgeResource(type: Int): Int {
    return when (type) {
      OVERLAY -> R.drawable.ic_abi_label_no_libs
      ERROR -> 0
      else -> if (isAbi64Bit(type % MULTI_ARCH)) R.drawable.ic_abi_label_64bit else R.drawable.ic_abi_label_32bit
    }
  }

  @DrawableRes
  fun getLargeAbiBadgeResource(type: Int): Int {
    return when (type % MULTI_ARCH) {
      ARMV8 -> R.drawable.ic_abi_label_arm64_v8a
      ARMV7 -> R.drawable.ic_abi_label_armeabi_v7a
      ARMV5 -> R.drawable.ic_abi_label_armeabi
      X86_64 -> R.drawable.ic_abi_label_x86_64
      X86 -> R.drawable.ic_abi_label_x86
      MIPS64 -> R.drawable.ic_abi_label_mips64
      MIPS -> R.drawable.ic_abi_label_mips
      RISCV64 -> R.drawable.ic_abi_label_riscv64
      RISCV32 -> R.drawable.ic_abi_label_riscv32
      ERROR -> 0
      else -> 0
    }
  }

  /**
   * Format size number to string
   * @param item LibStringItem
   * @return String of size number (100KB)
   */
  fun sizeToString(
    context: Context,
    item: LibStringItem
  ): String {
    return "(${item.size.sizeToString(context, showBytes = false)})"
  }

  /**
   * Format ELF type to string
   * @param type ELF type
   * @return String of ELF type
   */
  fun elfTypeToString(@ElfType type: Int): String {
    return when (type) {
      ET_NOT_SET -> ""
      ET_NOT_ELF -> "Broken ELF"
      ET_NONE -> "No File Type"
      ET_REL -> "Relocatable File"
      ET_EXEC -> "Executable File"
      ET_DYN -> "Shared Object File"
      ET_CORE -> "Core File"
      ET_LOPROC -> "Processor-specific"
      ET_HIPROC -> "Processor-specific"
      else -> "Not Standard ELF"
    }
  }

  fun getBuildVersionsInfo(
    packageInfo: PackageInfo?,
    packageName: String,
    isApexPackage: Boolean = false
  ): CharSequence {
    if (packageInfo == null && packageName != Constants.EXAMPLE_PACKAGE) {
      return ""
    }
    val showAndroidVersion =
      (GlobalValues.advancedOptions and AdvancedOptions.SHOW_ANDROID_VERSION) > 0
    val showTarget =
      (GlobalValues.advancedOptions and AdvancedOptions.SHOW_TARGET_API) > 0
    val showMin =
      (GlobalValues.advancedOptions and AdvancedOptions.SHOW_MIN_API) > 0
    val showCompile =
      (GlobalValues.advancedOptions and AdvancedOptions.SHOW_COMPILE_API) > 0
    val target = packageInfo?.applicationInfo?.targetSdkVersion ?: Build.VERSION.SDK_INT
    val min = packageInfo?.applicationInfo?.minSdkVersion ?: Build.VERSION.SDK_INT
    val compile = packageInfo?.getCompileSdkVersion() ?: Build.VERSION.SDK_INT

    return buildSpannedString {
      if (OsUtils.atLeastQ() && isApexPackage) {
        append(", APEX")
      }
      if (showTarget) {
        append(", ")
        scale(0.8f) {
          append("Target: ")
        }
        append(target.toString())
        if (showAndroidVersion) {
          append(" (${AndroidVersions.simpleVersions[target]})")
        }
      }

      if (showMin) {
        if (showTarget) {
          append(", ")
        }
        scale(0.8f) {
          append(" Min: ")
        }
        append(min.toString())
        if (showAndroidVersion) {
          append(" (${AndroidVersions.simpleVersions[min]})")
        }
      }

      if (showCompile) {
        if (showTarget || showMin) {
          append(", ")
        }
        scale(0.8f) {
          append(" Compile: ")
        }

        append(compile.toString().takeIf { it != "0" } ?: "?")
        if (showAndroidVersion) {
          append(" (${AndroidVersions.simpleVersions[compile] ?: "?"})")
        }
      }
    }
  }

  val ABI_STRING_RES_MAP = arrayMapOf(
    ERROR to listOf(R.string.cannot_read),
    NO_LIBS to listOf(R.string.no_libs),
    ARMV8 to listOf(R.string.arm64_v8a),
    X86_64 to listOf(R.string.x86_64),
    MIPS64 to listOf(R.string.mips64),
    RISCV64 to listOf(R.string.riscv64),
    ARMV7 to listOf(R.string.armeabi_v7a),
    ARMV5 to listOf(R.string.armeabi),
    X86 to listOf(R.string.x86),
    MIPS to listOf(R.string.mips),
    RISCV32 to listOf(R.string.riscv32),
    ARMV8 + MULTI_ARCH to listOf(R.string.arm64_v8a, R.string.multiArch),
    ARMV7 + MULTI_ARCH to listOf(R.string.armeabi_v7a, R.string.multiArch),
    ARMV5 + MULTI_ARCH to listOf(R.string.armeabi, R.string.multiArch),
    X86_64 + MULTI_ARCH to listOf(R.string.x86_64, R.string.multiArch),
    X86 + MULTI_ARCH to listOf(R.string.x86, R.string.multiArch),
    MIPS64 + MULTI_ARCH to listOf(R.string.mips64, R.string.multiArch),
    MIPS + MULTI_ARCH to listOf(R.string.mips, R.string.multiArch),
    RISCV64 + MULTI_ARCH to listOf(R.string.riscv64, R.string.multiArch),
    RISCV32 + MULTI_ARCH to listOf(R.string.riscv32, R.string.multiArch)
  )
}
