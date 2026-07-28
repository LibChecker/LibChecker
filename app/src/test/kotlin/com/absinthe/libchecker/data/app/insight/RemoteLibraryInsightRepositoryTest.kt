package com.absinthe.libchecker.data.app.insight

import com.absinthe.libchecker.api.request.RulesDocumentRequest
import com.absinthe.libchecker.domain.app.detail.insight.RemoteDocumentResult
import java.net.URI
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class RemoteLibraryInsightRepositoryTest {

  @Test
  fun `parses a short catalog response after repository fallback`() = runBlocking {
    val request = RecordingRulesDocumentRequest { url ->
      if (URI(url).host == "gitlab.com") {
        Response.error(404, "missing".toResponseBody())
      } else {
        Response.success(CATALOG_JSON.toResponseBody())
      }
    }

    val result = repository(request).getCatalog()

    assertTrue(result is RemoteDocumentResult.Success)
    val catalog = (result as RemoteDocumentResult.Success).value
    assertEquals("flutter", catalog.entries.single().sdkId)
    assertEquals(
      listOf("gitlab.com", "raw.githubusercontent.com"),
      request.requestedUrls.map { URI(it).host }
    )
  }

  @Test
  fun `parses short definition and lookup responses`() = runBlocking {
    val request = RecordingRulesDocumentRequest { url ->
      val body = when {
        url.endsWith("/definition.json") -> DEFINITION_JSON
        url.endsWith("/engine/revision.json") -> LOOKUP_JSON
        else -> error("Unexpected URL: $url")
      }
      Response.success(body.toResponseBody())
    }
    val repository = repository(request)

    val definition = repository.getDefinition("sdk-details/sdks/flutter/definition.json")
    val lookup = repository.getLookup("sdk-details/sdks/flutter/data/engine/revision.json")

    assertEquals(
      "flutter",
      (definition as RemoteDocumentResult.Success).value.sdkId
    )
    assertEquals(
      "3.24.5",
      (lookup as RemoteDocumentResult.Success).value["flutter"]
    )
  }

  @Test
  fun `rejects an oversized catalog response`() = runBlocking {
    val request = RecordingRulesDocumentRequest {
      Response.success("x".repeat(64 * 1024 + 1).toResponseBody())
    }

    val result = repository(request).getCatalog()

    assertEquals(RemoteDocumentResult.Failure, result)
  }

  private class RecordingRulesDocumentRequest(
    private val response: (String) -> Response<ResponseBody>
  ) : RulesDocumentRequest {

    val requestedUrls = mutableListOf<String>()

    override suspend fun get(url: String, referer: String): Response<ResponseBody> {
      requestedUrls += url
      return response(url)
    }
  }

  private fun repository(request: RulesDocumentRequest): RemoteLibraryInsightRepository {
    return RemoteLibraryInsightRepository(
      request = request,
      rulesRoots = {
        listOf(
          "https://gitlab.com/zhaobozhen/LibChecker-Rules/-/raw/v4/",
          "https://raw.githubusercontent.com/LibChecker/LibChecker-Rules/v4/"
        )
      }
    )
  }

  private companion object {
    val CATALOG_JSON = """
      {
        "schema_version": 1,
        "entries": [
          {
            "sdk_id": "flutter",
            "library_uuids": ["AEF9680F-4A43-4EDC-A5B8-8119D23BCD21"],
            "definition": "sdk-details/sdks/flutter/definition.json"
          }
        ]
      }
    """.trimIndent()

    val DEFINITION_JSON = """
      {
        "schema_version": 1,
        "sdk_id": "flutter",
        "target_uuids": ["AEF9680F-4A43-4EDC-A5B8-8119D23BCD21"],
        "probes": [],
        "lookups": [],
        "presentation": {
          "summary": [],
          "details": []
        }
      }
    """.trimIndent()

    val LOOKUP_JSON = """
      {
        "engine": "revision",
        "flutter": "3.24.5"
      }
    """.trimIndent()
  }
}
