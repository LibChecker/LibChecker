package com.absinthe.libchecker.api.request

import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.api.bean.LibDetailBean
import okhttp3.HttpUrl.Companion.toHttpUrl
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

interface LibDetailRequest {
  @GET
  suspend fun requestLibDetail(
    @Url url: String,
    @Header("Referer") referer: String = BuildConfig.APPLICATION_ID
  ): LibDetailBean
}

internal fun libraryDetailUrl(root: String, categoryDir: String, libName: String): String = root.toHttpUrl().newBuilder().addPathSegments(categoryDir).addPathSegments("$libName.json").build().toString()
