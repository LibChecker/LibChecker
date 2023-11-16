package com.absinthe.libchecker.api.request

import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.HEADER_BASE_URL
import com.absinthe.libchecker.api.bean.AndroidDistribution
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

const val ANDROID_DIST = "Android-Dist"

interface AndroidDistributionRequest {
  @Headers("$HEADER_BASE_URL: $ANDROID_DIST")
  @GET("{path}")
  suspend fun requestDistribution(@Path("path") path: String = ApiManager.ANDROID_VERSION_DISTRIBUTION_PATH): List<AndroidDistribution>
}
