package com.absinthe.libchecker.bean

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

const val DISABLED = "DISABLED"

@Parcelize
data class LibStringItem(
    @SerializedName("name") val name: String,
    @SerializedName("size") val size: Long = 0,
    @SerializedName("source") val source: String? = null
) : Parcelable