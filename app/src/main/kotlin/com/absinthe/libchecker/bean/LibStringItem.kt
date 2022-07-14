package com.absinthe.libchecker.bean

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

const val DISABLED = "DISABLED"

@Parcelize
@JsonClass(generateAdapter = true)
data class LibStringItem(
  val name: String,
  val size: Long = 0,
  val source: String? = null,
  val process: String? = null
) : Parcelable
