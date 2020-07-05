package com.absinthe.libchecker.bean

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class LibStringItem(
    @SerializedName("name") val name: String,
    @SerializedName("size") val size: Long = 0
) : Serializable