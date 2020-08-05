package com.absinthe.libchecker.api.request

import com.absinthe.libchecker.api.bean.NativeLibDetailBean
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface NativeLibDetailRequest {

    @GET("{categoryDir}/{libName}.json")
    fun requestNativeLibDetail(@Path("categoryDir") categoryDir: String, @Path("libName") libName: String): Call<NativeLibDetailBean>
}