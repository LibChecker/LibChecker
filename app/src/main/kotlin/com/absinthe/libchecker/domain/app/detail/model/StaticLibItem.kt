package com.absinthe.libchecker.domain.app.detail.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StaticLibItem(val name: String, val version: Int, val certDigest: String, var path: String = "")
