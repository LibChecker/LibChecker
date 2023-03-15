package com.absinthe.libchecker.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DetailExtraBean(
  val features: Int,
  val variant: Short,
) : Parcelable
