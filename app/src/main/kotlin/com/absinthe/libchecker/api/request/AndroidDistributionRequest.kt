package com.absinthe.libchecker.api.request

import com.absinthe.libchecker.api.HEADER_BASE_URL
import com.absinthe.libchecker.api.bean.AndroidDistribution
import retrofit2.http.GET
import retrofit2.http.Headers

const val ANDROID_DIST = "Android-Dist"

interface AndroidDistributionRequest {
  @Headers("$HEADER_BASE_URL: $ANDROID_DIST")
  @GET("android/studio/metadata/distributions.json")
  suspend fun requestDistribution(): List<AndroidDistribution>
}
