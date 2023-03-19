package com.absinthe.libchecker.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DetailExtraBean(
  val features: Int,
  val variant: Short
) : Parcelable
