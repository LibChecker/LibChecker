package com.absinthe.libchecker.api.request

import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.api.bean.LibDetailBean
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface LibDetailRequest {
  @GET("{categoryDir}/{libName}.json")
  suspend fun requestLibDetail(
    @Path("categoryDir") categoryDir: String,
    @Path("libName") libName: String,
    @Header("Referer") referer: String = BuildConfig.APPLICATION_ID
  ): LibDetailBean
}
