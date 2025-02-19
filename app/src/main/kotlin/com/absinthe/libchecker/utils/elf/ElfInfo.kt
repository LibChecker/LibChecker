package com.absinthe.libchecker.utils.elf

import android.os.Parcelable
import com.absinthe.libchecker.annotation.ET_NOT_ELF
import kotlinx.parcelize.Parcelize

@Parcelize
data class ElfInfo(
  val elfType: Int = ET_NOT_ELF,
  val elfClass: Int = ELFParser.EIdent.ELFCLASSNONE,
  val pageSize: Int = -1,
  val uncompressedAndNot16KB: Boolean = false
) : Parcelable {
  constructor(parser: ELFParser?, uncompressedAndNot16KB: Boolean = false) : this(
    parser?.getEType() ?: ET_NOT_ELF,
    parser?.getEClass() ?: ELFParser.EIdent.ELFCLASSNONE,
    parser?.getMinPageSize() ?: -1,
    uncompressedAndNot16KB
  )
}
