package com.absinthe.libchecker.bean

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class LibStringItem(
    @SerializedName("name") val name: String,
    @SerializedName("size") val size: Long = 0
) : Parcelable