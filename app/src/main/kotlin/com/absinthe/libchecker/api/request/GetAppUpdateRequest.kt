package com.absinthe.libchecker.api.request

import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.bean.GetAppUpdateInfo
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

private const val CHANNEL = "channel"

interface GetAppUpdateRequest {
  @GET("${ApiManager.ASSETS_REPO_BASE_URL}{$CHANNEL}.json")
  suspend fun requestAppUpdateInfo(
    @Path(CHANNEL) channel: String,
    @Header("Referer") referer: String = BuildConfig.APPLICATION_ID
  ): GetAppUpdateInfo?
}
