package com.absinthe.libchecker.bean

import android.os.Parcelable
import com.absinthe.libchecker.annotation.LibType
import kotlinx.parcelize.Parcelize

@Parcelize
data class LibReference(
  val libName: String,
  val chip: LibChip?,
  val referredList: Set<String>,
  @LibType val type: Int
) : Parcelable
