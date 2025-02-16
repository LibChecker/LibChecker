package com.absinthe.libchecker.utils.elf

import android.os.Parcelable
import com.absinthe.libchecker.annotation.ET_NOT_ELF
import com.absinthe.libchecker.utils.extensions.PAGE_SIZE_4_KB
import kotlinx.parcelize.Parcelize

@Parcelize
data class ElfInfo(
  val elfType: Int = ET_NOT_ELF,
  val elfClass: Int = ELFParser.EIdent.ELFCLASSNONE,
  val pageSize: Int = PAGE_SIZE_4_KB
) : Parcelable {
  constructor(parser: ELFParser?) : this(
    parser?.getEType() ?: ET_NOT_ELF,
    parser?.getEClass() ?: ELFParser.EIdent.ELFCLASSNONE,
    parser?.getPageSize() ?: PAGE_SIZE_4_KB
  )
}
