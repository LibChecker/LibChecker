package com.absinthe.libchecker.api.request

import com.absinthe.libchecker.utils.JsonUtil
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class LibDetailRequestTest {
  @Test fun usesSelectedRootAndLegacyPathsAndPropagatesFailuresForRetry() {
    val urls = mutableListOf<String>()
    var status = 200
    val client = OkHttpClient.Builder().addInterceptor { chain ->
      urls += chain.request().url.toString()
      Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
        .code(status).message("test").body("""{"uuid":"test","data":[]}""".toResponseBody()).build()
    }.build()
    val request = Retrofit.Builder().baseUrl("https://unused.example/").client(client)
      .addConverterFactory(MoshiConverterFactory.create(JsonUtil.moshi)).build().create(LibDetailRequest::class.java)
    val github = "https://raw.githubusercontent.com/LibChecker/LibChecker-Rules/v4/"
    val gitlab = "https://gitlab.com/zhaobozhen/LibChecker-Rules/-/raw/v4/"
    runBlocking {
      assertEquals("test", request.requestLibDetail(libraryDetailUrl(github, "native-libs", "libflutter.so")).uuid)
      request.requestLibDetail(libraryDetailUrl(gitlab, "services-libs/regex", "com/google/firebase/Service"))
    }
    assertEquals(listOf("${github}native-libs/libflutter.so.json", "${gitlab}services-libs/regex/com/google/firebase/Service.json"), urls)
    status = 404
    assertThrows(HttpException::class.java) {
      runBlocking { request.requestLibDetail(libraryDetailUrl(github, "native-libs", "missing")) }
    }
    status = 200
    runBlocking { assertEquals("test", request.requestLibDetail(libraryDetailUrl(github, "native-libs", "missing")).uuid) }
  }
}
