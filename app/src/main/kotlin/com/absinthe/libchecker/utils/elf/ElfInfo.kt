package com.absinthe.libchecker.utils.elf

import android.os.Parcelable
import com.absinthe.libchecker.annotation.ET_NOT_SET
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class ElfInfo(
  val elfType: Int = ET_NOT_SET,
  val pageSize: Int = -1,
  val uncompressedAndNot16KB: Boolean = false
) : Parcelable
