package com.absinthe.libchecker.api.request

import com.absinthe.libchecker.api.bean.NativeLibDetailBean
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Url

interface NativeLibDetailRequest {

    @GET
    fun requestNativeLibDetail(@Url url: String): Call<NativeLibDetailBean>
}