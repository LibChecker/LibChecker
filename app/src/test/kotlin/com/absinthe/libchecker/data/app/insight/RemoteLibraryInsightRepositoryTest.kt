package com.absinthe.libchecker.data.app.insight

import com.absinthe.libchecker.api.request.RulesDocumentRequest
import com.absinthe.libchecker.domain.app.detail.insight.RemoteDocumentResult
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.ForwardingSource
import okio.buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Response
import retrofit2.Retrofit

class RemoteLibraryInsightRepositoryTest {

  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `reuses fresh responses and revalidates stale responses across repository instances`() = runBlocking {
    for (maxAge in listOf(3600, 0)) {
      val requests = AtomicInteger()
      val validations = AtomicInteger()
      val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
      server.createContext("/") { exchange ->
        exchange.use {
          requests.incrementAndGet()
          exchange.responseHeaders.add("Cache-Control", "max-age=$maxAge")
          exchange.responseHeaders.add("ETag", "\"revision-1\"")
          if (exchange.requestHeaders.getFirst("If-None-Match") == "\"revision-1\"") {
            validations.incrementAndGet()
            exchange.sendResponseHeaders(304, -1)
          } else {
            val bytes = LOOKUP_JSON.toByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.write(bytes)
          }
        }
      }
      server.start()
      try {
        val root = "http://127.0.0.1:${server.address.port}/"
        val directory = temporaryFolder.newFolder()
        repeat(2) {
          Cache(directory, 1024 * 1024).use { cache ->
            val client = OkHttpClient.Builder().cache(cache).build()
            val request = Retrofit.Builder().baseUrl(root).client(client).build()
              .create(RulesDocumentRequest::class.java)
            val result = RemoteLibraryInsightRepository(request) { listOf(root) }
              .getLookup("sdk-details/sdks/flutter/data/index.json")
            assertEquals("3.24.5", (result as RemoteDocumentResult.Success).value["flutter"])
            client.connectionPool.evictAll()
            client.dispatcher.executorService.shutdown()
          }
        }
        assertEquals(if (maxAge == 0) 2 else 1, requests.get())
        assertEquals(if (maxAge == 0) 1 else 0, validations.get())
      } finally {
        server.stop(0)
      }
    }
  }

  @Test
  fun `streams unknown length responses off the caller thread and stops at the limit`() = runBlocking {
    val callerThread = Thread.currentThread()
    var bytesRead = 0L
    var closed = false
    val source = object : ForwardingSource(Buffer().writeUtf8("x".repeat(2 * 1024 * 1024))) {
      override fun read(sink: Buffer, byteCount: Long): Long {
        assertNotSame(callerThread, Thread.currentThread())
        return super.read(sink, byteCount).also { if (it > 0) bytesRead += it }
      }

      override fun close() {
        closed = true
        super.close()
      }
    }.buffer()
    val body = object : ResponseBody() {
      override fun contentType() = null
      override fun contentLength() = -1L
      override fun source() = source
    }
    val client = OkHttpClient.Builder().addInterceptor { chain ->
      okhttp3.Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
        .code(200).message("OK").body(body).build()
    }.build()
    try {
      val root = "https://example.com/"
      val request = Retrofit.Builder().baseUrl(root).client(client).build()
        .create(RulesDocumentRequest::class.java)
      val result = RemoteLibraryInsightRepository(request) { listOf(root) }
        .getLookup("sdk-details/sdks/flutter/data/index.json")
      assertEquals(RemoteDocumentResult.Failure, result)
      assertTrue(bytesRead in (1024L * 1024 + 1)..(1024L * 1024 + 8192))
      assertTrue(closed)
    } finally {
      client.dispatcher.executorService.shutdown()
    }
  }

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
  fun `parses version 2 prefixed and digest captures`() = runBlocking {
    val request = RecordingRulesDocumentRequest {
      Response.success(V2_DEFINITION_JSON.toResponseBody())
    }

    val result = repository(request)
      .getDefinition("sdk-details/sdks/androidx_media3/definition.json")

    val definition = (result as RemoteDocumentResult.Success).value
    assertEquals(2, definition.schemaVersion)
    assertEquals("AndroidXMedia3/", definition.probes[0].captures.single().prefix)
    assertEquals("file_sha256", definition.probes[1].reader.operator)
    assertEquals("sha256", definition.probes[1].captures.single().type)
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

    val V2_DEFINITION_JSON = """
      {
        "schema_version": 2,
        "sdk_id": "androidx_media3",
        "target_uuids": ["17C5D1C7-8A5A-412F-8F4D-C16EC303F663"],
        "probes": [
          {
            "id": "media3",
            "source": {
              "operator": "package_file",
              "file_name": "classes.dex",
              "archive_paths": ["classes.dex"]
            },
            "reader": {
              "operator": "ascii_strings",
              "max_bytes_per_file": 1024,
              "max_total_bytes": 2048
            },
            "captures": [
              {
                "output": "versions",
                "type": "prefixed_semver",
                "prefix": "AndroidXMedia3/",
                "max_results": 2
              }
            ]
          },
          {
            "id": "fingerprint",
            "source": {
              "operator": "package_file",
              "file_name": "module.kotlin_module",
              "archive_paths": ["META-INF/module.kotlin_module"]
            },
            "reader": {
              "operator": "file_sha256",
              "max_bytes_per_file": 1024,
              "max_total_bytes": 2048
            },
            "captures": [
              {
                "output": "fingerprints",
                "type": "sha256",
                "max_results": 2
              }
            ]
          }
        ],
        "presentation": {
          "summary": [
            {
              "label": {"default": "Version"},
              "source": "versions",
              "max_values": 2
            }
          ]
        }
      }
    """.trimIndent()
  }
}
