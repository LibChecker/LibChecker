package com.absinthe.libchecker.api.request

import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.api.HEADER_BASE_URL
import com.absinthe.libchecker.api.bean.GetAppUpdateInfo
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers

const val APP_UPDATE_HOST = "App-Update-Host"
const val CHANNEL = "Channel"

interface GetAppUpdateRequest {
  @Headers("$HEADER_BASE_URL: $APP_UPDATE_HOST")
  @GET("{$CHANNEL}.json")
  suspend fun requestAppUpdateInfo(
    @Header(CHANNEL) channel: String,
    @Header("Referer") referer: String = BuildConfig.APPLICATION_ID
  ): GetAppUpdateInfo?
}
