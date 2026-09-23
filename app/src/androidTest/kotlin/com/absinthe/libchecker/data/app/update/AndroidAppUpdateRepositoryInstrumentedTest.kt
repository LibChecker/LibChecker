package com.absinthe.libchecker.data.app.update

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.api.request.GetAppUpdateRequest
import com.absinthe.libchecker.domain.app.update.AppUpdateChannel
import com.absinthe.libchecker.utils.JsonUtil
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@RunWith(AndroidJUnit4::class)
class AndroidAppUpdateRepositoryInstrumentedTest {

  @Test
  fun failedGitHubApiTriesDirectRawBeforeCachedProxy() = runBlocking {
    val hosts = mutableListOf<String>()
    var directAvailable = true
    val client = OkHttpClient.Builder().addInterceptor { chain ->
      val request = chain.request()
      hosts += request.url.host
      val unavailable = request.url.host == "api.github.com" ||
        (request.url.host == "raw.githubusercontent.com" && !directAvailable)
      Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(if (unavailable) 503 else 200)
        .message("test")
        .body(UPDATE_JSON.toResponseBody("application/json".toMediaType()))
        .build()
    }.build()
    val request = Retrofit.Builder()
      .baseUrl("https://unused.example/")
      .client(client)
      .addConverterFactory(MoshiConverterFactory.create(JsonUtil.moshi))
      .build()
      .create(GetAppUpdateRequest::class.java)
    val repository = AndroidAppUpdateRepository(
      context = InstrumentationRegistry.getInstrumentation().targetContext,
      request = request
    )

    assertEquals(3000, repository.requestUpdateInfo(AppUpdateChannel.CI)?.app?.versionCode)
    assertEquals(listOf("api.github.com", "raw.githubusercontent.com"), hosts)

    hosts.clear()
    directAvailable = false
    assertEquals(3000, repository.requestUpdateInfo(AppUpdateChannel.CI)?.app?.versionCode)
    assertEquals(listOf("api.github.com", "raw.githubusercontent.com", "gh.absinthe.life"), hosts)
  }

  private companion object {
    const val UPDATE_JSON = """{"app":{"version":"2.5.6","versionCode":3000,"extra":{"target":37,"min":24,"compile":37,"packageSize":1},"link":"https://example.com/app.apk","note":null}}"""
  }
}
