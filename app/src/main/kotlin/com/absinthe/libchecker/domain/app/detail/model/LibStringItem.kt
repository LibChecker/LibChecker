package com.absinthe.libchecker.domain.app.detail.model

import android.os.Parcelable
import com.absinthe.libchecker.utils.elf.ElfInfo
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
  val elfInfo: ElfInfo = ElfInfo()
) : Parcelable
