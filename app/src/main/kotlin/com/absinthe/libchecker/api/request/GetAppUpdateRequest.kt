package com.absinthe.libchecker.api.request

import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.bean.GetAppUpdateInfo
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Path

private const val CHANNEL = "channel"

interface GetAppUpdateRequest {
  @Headers("Accept: application/vnd.github.raw")
  @GET("${ApiManager.ASSETS_REPO_BASE_URL}{$CHANNEL}.json?ref=main")
  suspend fun requestAppUpdateInfo(
    @Path(CHANNEL) channel: String,
    @Header("Authorization") authorization: String? = null,
    @Header("Referer") referer: String = BuildConfig.APPLICATION_ID
  ): GetAppUpdateInfo?

  @GET("${ApiManager.ASSETS_REPO_FALLBACK_BASE_URL}{$CHANNEL}.json")
  suspend fun requestFallbackAppUpdateInfo(
    @Path(CHANNEL) channel: String,
    @Header("Referer") referer: String = BuildConfig.APPLICATION_ID
  ): GetAppUpdateInfo?
}
