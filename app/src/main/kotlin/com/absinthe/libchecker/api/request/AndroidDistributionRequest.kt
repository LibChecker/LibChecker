package com.absinthe.libchecker.api.request

import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.bean.AndroidDistribution
import retrofit2.http.GET

interface AndroidDistributionRequest {
  @GET(ApiManager.ANDROID_VERSION_DISTRIBUTION_URL)
  suspend fun requestDistribution(): List<AndroidDistribution>
}
