package com.absinthe.libchecker.constant

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class LibChip(
    val iconRes: Int,
    val name: String,
    val regexName: String? = null
) : Parcelable