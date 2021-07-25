package com.absinthe.libchecker.api.request

import com.absinthe.libchecker.api.bean.LibDetailBean
import retrofit2.http.GET
import retrofit2.http.Path

interface LibDetailRequest {
    @GET("{categoryDir}/{libName}.json")
    suspend fun requestLibDetail(
        @Path("categoryDir") categoryDir: String,
        @Path("libName") libName: String
    ): LibDetailBean
}
