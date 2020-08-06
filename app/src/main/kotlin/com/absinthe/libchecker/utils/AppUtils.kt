package com.absinthe.libchecker.utils

import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.bean.Configuration
import com.absinthe.libchecker.api.request.ConfigurationRequest
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.ktx.logd
import com.absinthe.libchecker.ktx.loge
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object AppUtils {

    /**
     * Request online configuration to control functions visibility
     */
    fun requestConfiguration() {
        val retrofit = Retrofit.Builder()
            .baseUrl(ApiManager.root)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val request = retrofit.create(ConfigurationRequest::class.java)
        val config = request.requestConfiguration()

        config.enqueue(object : Callback<Configuration> {
            override fun onFailure(call: Call<Configuration>, t: Throwable) {
                loge(t.message ?: "")
            }

            override fun onResponse(call: Call<Configuration>, response: Response<Configuration>) {
                response.body()?.let {
                    logd("Configuration response: ${response.body()}")
                    GlobalValues.config = it
                } ?: loge(response.message())
            }
        })
    }

}