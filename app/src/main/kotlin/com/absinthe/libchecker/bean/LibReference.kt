package com.absinthe.libchecker.bean

import android.os.Parcelable
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.constant.LibChip
import kotlinx.android.parcel.Parcelize

@Parcelize
data class LibReference(
    val libName: String,
    val chip: LibChip?,
    val referredCount: Int,
    @LibType val type :Int
) : Parcelable