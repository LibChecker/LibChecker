package com.absinthe.libchecker.domain.app

import android.content.Context
import com.absinthe.libchecker.annotation.ET_DYN
import com.absinthe.libchecker.annotation.ET_NOT_ELF
import com.absinthe.libchecker.annotation.ET_NOT_SET
import com.absinthe.libchecker.domain.app.detail.model.LibStringItem
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.PAGE_SIZE_16_KB

class BuildNativeLibraryItemDisplayDataUseCase(
  private val context: Context
) {

  operator fun invoke(
    item: LibStringItem,
    labels: List<String>
  ): NativeLibraryItemDisplayData {
    val elfInfo = item.elfInfo
    return NativeLibraryItemDisplayData(
      sizeText = PackageUtils.sizeToString(context, item),
      labels = buildList {
        if (elfInfo.elfType != ET_NOT_SET && elfInfo.elfType != ET_DYN) {
          add(PackageUtils.elfTypeToString(elfInfo.elfType))
        }
        if (elfInfo.elfType != ET_NOT_ELF) {
          if (elfInfo.pageSize > 0 && elfInfo.pageSize % PAGE_SIZE_16_KB == 0) {
            add("16 KB")
          }
          getZipAlignmentText(elfInfo.zipAlignment)?.let(::add)
        }
        addAll(labels)
      }
    )
  }

  private fun getZipAlignmentText(zipAlignment: Long): String? {
    if (zipAlignment <= 0L || zipAlignment >= PAGE_SIZE_16_KB) {
      return null
    }
    return if (zipAlignment >= 1024L && zipAlignment % 1024L == 0L) {
      "${zipAlignment / 1024}KB ZIPALIGN"
    } else {
      "${zipAlignment}B ZIPALIGN"
    }
  }
}

data class NativeLibraryItemDisplayData(
  val sizeText: String,
  val labels: List<String>
)
