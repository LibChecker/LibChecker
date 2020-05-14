package com.absinthe.libchecker.api.request

import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.Configuration
import retrofit2.Call
import retrofit2.http.GET

interface ConfigurationRequest {

    @GET(ApiManager.CONFIGURATION_URL)
    fun requestConfiguration(): Call<Configuration>

}