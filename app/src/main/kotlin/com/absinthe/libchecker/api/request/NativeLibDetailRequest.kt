package com.absinthe.libchecker.api.request

import retrofit2.http.GET
import retrofit2.http.Url

interface NativeLibDetailRequest {

    @GET
    fun requestNativeLibDetail(@Url url: String)
}