package com.absinthe.libchecker.api.request

import com.absinthe.libchecker.BuildConfig
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

interface RulesDocumentRequest {
  @GET
  suspend fun get(
    @Url url: String,
    @Header("Referer") referer: String = BuildConfig.APPLICATION_ID
  ): Response<ResponseBody>
}
