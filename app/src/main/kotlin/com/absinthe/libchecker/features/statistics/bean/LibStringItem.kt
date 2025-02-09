package com.absinthe.libchecker.features.statistics.bean

import android.os.Parcelable
import com.absinthe.libchecker.annotation.ET_NOT_ELF
import com.absinthe.libchecker.annotation.ElfType
import com.absinthe.libchecker.utils.elf.ELFParser
import com.absinthe.libchecker.utils.extensions.PAGE_SIZE_4_KB
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

const val DISABLED = "DISABLED"
const val EXPORTED = "EXPORTED"

@Parcelize
@JsonClass(generateAdapter = true)
data class LibStringItem(
  val name: String,
  val size: Long = 0,
  val source: String? = null,
  val process: String? = null,
  @ElfType val elfType: Int = ET_NOT_ELF,
  val elfClass: Int = ELFParser.EIdent.ELFCLASSNONE,
  val pageSize: Int = PAGE_SIZE_4_KB
) : Parcelable
