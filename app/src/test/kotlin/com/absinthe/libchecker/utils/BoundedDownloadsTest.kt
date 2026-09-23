package com.absinthe.libchecker.utils

import com.absinthe.libchecker.data.statistics.HttpOfficialStatisticRemoteSource
import com.sun.net.httpserver.HttpServer
import java.io.IOException
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BoundedDownloadsTest {
  @get:Rule val temporary = TemporaryFolder()

  @Test
  fun `known and chunked downloads retain size limits and failure handling`() {
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
      val bytes = if (exchange.requestURI.path.endsWith("empty")) byteArrayOf() else "data".toByteArray()
      val knownLength = exchange.requestURI.path.startsWith("/known")
      if (knownLength) exchange.responseHeaders.set("Content-Length", bytes.size.toString())
      exchange.sendResponseHeaders(200, if (knownLength) bytes.size.toLong().takeIf { it > 0 } ?: -1 else 0)
      exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    val client = OkHttpClient()
    try {
      for (knownLength in listOf(true, false)) {
        for (body in listOf("", "data")) {
          for (maximum in listOf(-2L, -1L, 0L, 1L, 4L, 5L)) {
            val path = "${if (knownLength) "known" else "chunked"}/${if (body.isEmpty()) "empty" else "data"}"
            val url = "http://127.0.0.1:${server.address.port}/$path"
            val expected = (if (knownLength) body.length.toLong() else -1L) <= maximum &&
              (body.isEmpty() || body.length <= maximum)
            val destination = temporary.newFolder().resolve("download")
            val finished = CountDownLatch(1)
            var succeeded = false
            DownloadUtils.download(
              url,
              destination,
              object : DownloadUtils.OnDownloadListener {
                override fun onDownloadSuccess() {
                  succeeded = true
                  finished.countDown()
                }

                override fun onDownloadFailed() {
                  finished.countDown()
                }
              },
              maximum
            )
            assertTrue("Download callback timed out", finished.await(10, TimeUnit.SECONDS))
            assertEquals("$path limit=$maximum", expected, succeeded)
            if (expected) assertArrayEquals(body.toByteArray(), destination.readBytes()) else assertFalse(destination.exists())

            val chartDestination = temporary.newFolder().resolve("chart")
            val source = HttpOfficialStatisticRemoteSource(client, url, url)
            if (expected) {
              runBlocking { source.downloadBundle(chartDestination, maximum) }
              assertArrayEquals(body.toByteArray(), chartDestination.readBytes())
            } else {
              val failure = assertThrows(IOException::class.java) {
                runBlocking { source.downloadBundle(chartDestination, maximum) }
              }
              assertEquals("Chart bundle is larger than its manifest", failure.message)
              assertEquals(!knownLength && maximum >= -1, chartDestination.exists())
            }
          }
        }
      }
    } finally {
      client.connectionPool.evictAll()
      client.dispatcher.executorService.shutdown()
      server.stop(0)
    }
  }
}
