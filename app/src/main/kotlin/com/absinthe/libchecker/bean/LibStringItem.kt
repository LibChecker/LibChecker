package com.absinthe.libchecker.bean

import android.os.Parcelable
import com.absinthe.libchecker.annotation.ET_NOT_ELF
import com.absinthe.libchecker.annotation.ElfType
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

const val DISABLED = "DISABLED"
const val NOT_EXPORTED = "NOT_EXPORTED"

@Parcelize
@JsonClass(generateAdapter = true)
data class LibStringItem(
  val name: String,
  val size: Long = 0,
  val source: String? = null,
  val process: String? = null,
  @ElfType val elfType: Int = ET_NOT_ELF
) : Parcelable
